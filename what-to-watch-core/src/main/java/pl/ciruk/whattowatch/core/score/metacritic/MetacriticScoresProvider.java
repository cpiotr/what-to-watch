package pl.ciruk.whattowatch.core.score.metacritic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MetacriticScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String METACRITIC = "Metacritic";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NOT_IN_LINK = Pattern.compile("[^a-z0-9 -]");
    private static final Pattern SEPARATORS = Pattern.compile("[ -]+");

    private final HttpConnection<Element> connection;
    private final ExecutorService executorService;
    private final AtomicLong missingScores = new AtomicLong();

    public MetacriticScoresProvider(
            HttpConnection<Element> connection,
            ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(METACRITIC)),
                missingScores,
                AtomicLong::get
        );
    }

    @Override
    public CompletableFuture<Stream<Score>> findScoresByAsync(Description description) {
        return CompletableFuture.supplyAsync(
                () -> findScoresBy(description),
                executorService
        );
    }

    @Override
    public Stream<Score> findScoresBy(Description description) {
        LOGGER.debug("Description: {}", description);

        var title = description.getTitle();
        var link = linkFor(title.asText());
        // Metacritic gives a later film with the same title a link that ends with its year
        var score = findScore(link, title)
                .or(() -> findScore(link + "-" + title.year(), title));
        if (score.isEmpty()) {
            LOGGER.warn("Missing Metacritic score for: {}", title);
            missingScores.incrementAndGet();
        }

        return score.stream()
                .peek(found -> LOGGER.debug("Score for {}: {}", description, found));
    }

    static String linkFor(String title) {
        var withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(title, Normalizer.Form.NFD)).replaceAll("");
        var lettersAndDigits = NOT_IN_LINK.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("");
        return SEPARATORS.matcher(lettersAndDigits.strip()).replaceAll("-");
    }

    private Optional<Score> findScore(String link, Title title) {
        var url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.metacritic.com")
                .addPathSegment("movie")
                .addPathSegment(link)
                .addPathSegment("")
                .build();

        return connection.connectToAndGet(url)
                .flatMap(MetacriticScoresProvider::findFilmData)
                .filter(film -> titleOf(film).matches(title))
                .flatMap(film -> scoreOf(film, url));
    }

    // The film page describes the film and its Metascore as schema.org data
    private static Optional<JsonNode> findFilmData(Element page) {
        return page.select("script[type=application/ld+json]").stream()
                .map(script -> readJson(script.data()))
                .flatMap(Optional::stream)
                .filter(json -> "Movie".equals(json.path("@type").asText()))
                .findFirst();
    }

    private static Optional<JsonNode> readJson(String json) {
        try {
            return Optional.of(OBJECT_MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            LOGGER.debug("Could not parse schema.org data", e);
            return Optional.empty();
        }
    }

    private static Title titleOf(JsonNode film) {
        var name = film.path("name").asText();
        var releaseDate = film.path("datePublished").asText();
        var year = releaseDate.length() >= 4 ? Integer.parseInt(releaseDate.substring(0, 4)) : Title.MISSING_YEAR;
        return Title.builder()
                .title(name)
                .originalTitle(name)
                .year(year)
                .build();
    }

    private static Optional<Score> scoreOf(JsonNode film, HttpUrl url) {
        var rating = film.path("aggregateRating");
        var metascore = rating.path("ratingValue").asDouble();
        var numberOfReviews = rating.path("reviewCount").asInt();
        if (metascore <= 0 || numberOfReviews <= 0) {
            return Optional.empty();
        }

        return Optional.of(Score.builder()
                .grade(metascore / 100.0)
                .quantity(numberOfReviews)
                .source(METACRITIC)
                .type(ScoreType.CRITIC)
                .url(url.toString())
                .build());
    }
}
