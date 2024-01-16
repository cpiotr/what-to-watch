package pl.ciruk.whattowatch.core.score.metacritic;

import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class MetacriticScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final HttpConnection<Element> connection;
    private final ExecutorService executorService;
    private final AtomicLong missingMetacriticScores = new AtomicLong();
    private final AtomicLong missingNewYorkTimesScores = new AtomicLong();

    public MetacriticScoresProvider(
            HttpConnection<Element> connection,
            ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(MetacriticScoreUtil.METACRITIC)),
                missingMetacriticScores,
                AtomicLong::get
        );

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(NewYorkTimesScoreUtil.NEW_YORK_TIMES)),
                missingNewYorkTimesScores,
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

        final var element = metacriticSummaryOf(description.getTitle());

        return element.stream().flatMap(htmlWithScores -> findScores(htmlWithScores, description));
    }

    private Stream<Score> findScores(Element htmlWithScores, Description description) {
        var metacriticScoreBuilder = MetacriticScoreUtil.extractToScoreBuilder(htmlWithScores);
        if (metacriticScoreBuilder.isEmpty()) {
            LOGGER.warn("Missing Metacritic score for: {}", description.getTitle());
            missingMetacriticScores.incrementAndGet();
        }

        var nytScoreBuilder = NewYorkTimesScoreUtil.extractToScoreBuilder(htmlWithScores);
        if (nytScoreBuilder.isEmpty()) {
            LOGGER.warn("Missing NYT score for: {}", description.getTitle());
            missingNewYorkTimesScores.incrementAndGet();
        }

        return Stream.concat(metacriticScoreBuilder.stream(), nytScoreBuilder.stream())
                .map(scoreBuilder -> scoreBuilder.url(resolve("")).build())
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score));
    }

    private String resolve(String link) {
        HttpUrl httpUrl = metacriticUrlBuilder().build();
        return Optional.ofNullable(httpUrl.resolve(link))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Could not resolve: " + link));
    }

    private Optional<Element> getSearchResultsFor(Title title) {
        var url = metacriticUrlBuilder()
                .addPathSegments("movie")
                .addPathSegment(title.asText().replace(":", "").replace(' ', '-').toLowerCase(Locale.ROOT))
                .addPathSegment("critic-reviews")
                .build();
        return connection.connectToAndGet(url);
    }

    private HttpUrl.Builder metacriticUrlBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("www.metacritic.com");
    }

    private Optional<Element> metacriticSummaryOf(Title title) {
        try {
            return getSearchResultsFor(title);
        } catch (Exception e) {
            LOGGER.warn("Cannot find metacritic summary of {}", title, e);
            return Optional.empty();
        }
    }
}
