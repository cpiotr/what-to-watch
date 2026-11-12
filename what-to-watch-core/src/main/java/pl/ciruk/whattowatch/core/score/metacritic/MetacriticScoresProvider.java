package pl.ciruk.whattowatch.core.score.metacritic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.text.Texts;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MetacriticScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String METACRITIC = "Metacritic";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern NOT_IN_LINK = Pattern.compile("[^a-z0-9 -]");
    private static final Pattern SEPARATORS = Pattern.compile("[ -]+");

    private final HttpConnection<Element> connection;
    private final Counter missingScores = Metrics.counter(
            Names.getNameForMissingScores(),
            List.of(Tags.getProviderTag(METACRITIC)));

    public MetacriticScoresProvider(HttpConnection<Element> connection) {
        this.connection = connection;
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
            missingScores.increment();
        }

        return score.stream()
                .peek(found -> LOGGER.debug("Score for {}: {}", description, found));
    }

    static String linkFor(String title) {
        var lettersAndDigits = NOT_IN_LINK.matcher(Texts.withoutDiacritics(title).toLowerCase(Locale.ROOT)).replaceAll("");
        return SEPARATORS.matcher(lettersAndDigits.strip()).replaceAll("-");
    }

    private Optional<Score> findScore(String link, Title title) {
        var url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.metacritic.com")
                .addPathSegment(sectionOf(title.kind()))
                .addPathSegment(link)
                .addPathSegment("")
                .build();

        return connection.get(url)
                .flatMap(page -> findTitleData(page, title.kind()))
                .filter(data -> titleOf(data, title.kind()).matches(title))
                .flatMap(data -> scoreOf(data, url));
    }

    private static String sectionOf(TitleKind kind) {
        return switch (kind) {
            case FILM -> "movie";
            case SERIES -> "tv";
        };
    }

    // The page describes the film or series and its Metascore as schema.org data
    private static Optional<JsonNode> findTitleData(Element page, TitleKind kind) {
        return page.select("script[type=application/ld+json]").stream()
                .map(script -> readJson(script.data()))
                .flatMap(Optional::stream)
                .filter(json -> schemaTypeOf(kind).equals(json.path("@type").asText()))
                .findFirst();
    }

    private static String schemaTypeOf(TitleKind kind) {
        return switch (kind) {
            case FILM -> "Movie";
            case SERIES -> "TVSeries";
        };
    }

    private static Optional<JsonNode> readJson(String json) {
        try {
            return Optional.of(OBJECT_MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            LOGGER.debug("Could not parse schema.org data", e);
            return Optional.empty();
        }
    }

    private static Title titleOf(JsonNode data, TitleKind kind) {
        var name = data.path("name").asText();
        var releaseDate = data.path("datePublished").asText();
        var year = releaseDate.length() >= 4 ? Integer.parseInt(releaseDate.substring(0, 4)) : Title.MISSING_YEAR;
        return Title.builder()
                .title(name)
                .originalTitle(name)
                .year(year)
                .kind(kind)
                .build();
    }

    private static Optional<Score> scoreOf(JsonNode data, HttpUrl url) {
        var rating = data.path("aggregateRating");
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
