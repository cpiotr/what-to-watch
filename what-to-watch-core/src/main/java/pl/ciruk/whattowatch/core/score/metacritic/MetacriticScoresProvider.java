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
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.core.score.metacritic.MetacriticSelectors.LINK_TO_DETAILS;
import static pl.ciruk.whattowatch.core.title.Title.MISSING_YEAR;

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
                Names.createName(ScoresProvider.class, List.of("metacritic", "missing", "count")),
                missingMetacriticScores,
                AtomicLong::get
        );

        Metrics.gauge(
                Names.createName(ScoresProvider.class, List.of("newYorkTimes", "missing", "count")),
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

        return metacriticSummaryOf(description.getTitle())
                .flatMap(LINK_TO_DETAILS::extractFrom)
                .stream()
                .flatMap(linkToDetails -> findScores(linkToDetails, description));
    }

    private Stream<Score> findScores(String linkToDetails, Description description) {
        var htmlWithScores = getPage(linkToDetails, "critic-reviews")
                .map(this::extractCriticReviews)
                .or(() -> followDetailsLinkAndFindPageWithScores(linkToDetails));

        var metacriticScoreBuilder = htmlWithScores
                .flatMap(MetacriticScoreUtil::extractToScoreBuilder);
        if (metacriticScoreBuilder.isEmpty()) {
            LOGGER.warn("Missing Metacritic score for: {}", description.getTitle());
            missingMetacriticScores.incrementAndGet();
        }

        var nytScoreBuilder = htmlWithScores.flatMap(NewYorkTimesScoreUtil::extractToScoreBuilder);
        if (nytScoreBuilder.isEmpty()) {
            LOGGER.warn("Missing NYT score for: {}", description.getTitle());
            missingNewYorkTimesScores.incrementAndGet();
        }

        return Stream.concat(metacriticScoreBuilder.stream(), nytScoreBuilder.stream())
                .map(scoreBuilder -> scoreBuilder.url(resolve(linkToDetails)).build())
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score));
    }

    private Optional<Element> followDetailsLinkAndFindPageWithScores(String linkToDetails) {
        return getPage(linkToDetails)
                .flatMap(MetacriticSelectors.LINK_TO_CRITIC_REVIEWS::extractFrom)
                .flatMap(this::getPage)
                .map(this::extractCriticReviews);
    }

    private Element extractCriticReviews(Element page) {
        return page.select("#main_content .critic_reviews").first();
    }

    private String resolve(String link) {
        HttpUrl httpUrl = metacriticUrlBuilder().build();
        return Optional.ofNullable(httpUrl.resolve(link))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Could not resolve: " + link));
    }

    private Optional<Element> getSearchResultsFor(Title title) {
        return getPage("search", "movie", title.asText(), "results");
    }

    private Optional<Element> getPage(String firstSegment, String... pathSegments) {
        var builder = metacriticUrlBuilder()
                .build()
                .newBuilder(firstSegment);
        Arrays.stream(pathSegments).forEach(builder::addPathSegments);
        var url = builder.build();
        return connection.connectToAndGet(url);
    }

    private HttpUrl.Builder metacriticUrlBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("www.metacritic.com");
    }

    private Optional<Element> metacriticSummaryOf(Title title) {
        try {
            return getSearchResultsFor(title)
                    .flatMap(searchResults -> findFirstResultMatching(title, searchResults));
        } catch (Exception e) {
            LOGGER.warn("Cannot find metacritic summary of {}", title, e);
            return Optional.empty();
        }
    }

    private Optional<Element> findFirstResultMatching(Title title, Element searchResults) {
        return MetacriticStreamSelectors.SEARCH_RESULTS.extractFrom(searchResults)
                .filter(e -> extractTitle(e).matches(title))
                .findFirst();
    }

    private Title extractTitle(Element searchResult) {
        var title = MetacriticSelectors.TITLE.extractFrom(searchResult).orElse("");
        var year = MetacriticSelectors.RELEASE_YEAR.extractFrom(searchResult)
                .map(Integer::parseInt)
                .orElse(MISSING_YEAR);

        return Title.builder()
                .title(title)
                .year(year)
                .build();
    }
}
