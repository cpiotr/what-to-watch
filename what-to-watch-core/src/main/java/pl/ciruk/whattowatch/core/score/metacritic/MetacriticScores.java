package pl.ciruk.whattowatch.core.score.metacritic;

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
import pl.ciruk.whattowatch.utils.math.Doubles;
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
import static pl.ciruk.whattowatch.utils.stream.Optionals.mergeUsing;

@SuppressWarnings("PMD.TooManyMethods") // TODO
public class MetacriticScores implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int NYT_SCORE_WEIGHT = 10;

    private final HttpConnection<Element> connection;

    private final ExecutorService executorService;

    private final AtomicLong missingMetacriticScores = new AtomicLong();

    private final AtomicLong missingNewYorkTimesScores = new AtomicLong();

    public MetacriticScores(
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

        var optionalLinkToDetails = metacriticSummaryOf(description.getTitle())
                .flatMap(LINK_TO_DETAILS::extractFrom);
        if (optionalLinkToDetails.isEmpty()) {
            return Stream.empty();
        }

        String linkToDetails = optionalLinkToDetails.get();
        var htmlWithScores = getCriticScoresFor(linkToDetails)
                .map(this::extractCriticReviews)
                .or(() -> followDetailsLinkAndFindPageWithScores(linkToDetails));

        var metacriticScore = htmlWithScores
                .flatMap(this::extractScoreFrom);
        if (metacriticScore.isEmpty()) {
            LOGGER.warn("Missing Metacritic score for: {}", description.getTitle());
            missingMetacriticScores.incrementAndGet();
        }

        var nytScore = htmlWithScores.flatMap(this::nytScoreFrom);
        if (nytScore.isEmpty()) {
            LOGGER.warn("Missing NYT score for: {}", description.getTitle());
            missingNewYorkTimesScores.incrementAndGet();
        }

        var averageScoreStream = metacriticScore.stream();
        var nytScoreStream = nytScore.stream();
        return Stream.concat(averageScoreStream, nytScoreStream)
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

    private Optional<Score.ScoreBuilder> extractScoreFrom(Element htmlWithScores) {
        var averageGrade = averageGradeFrom(htmlWithScores);
        var numberOfReviews = numberOfReviewsFrom(htmlWithScores);
        return mergeUsing(
                averageGrade,
                numberOfReviews,
                this::createScore);
    }

    private Score.ScoreBuilder createScore(Double rating, Double count) {
        return Score.builder()
                .grade(rating)
                .quantity(count.intValue())
                .source("Metacritic")
                .type(ScoreType.CRITIC);
    }

    private String resolve(String link) {
        return metacriticUrlBuilder().build().resolve(link).toString();
    }

    private Optional<Double> averageGradeFrom(Element htmlWithScores) {
        return MetacriticStreamSelectors.CRITIC_REVIEWS.extractFrom(htmlWithScores)
                .map(Element::text)
                .mapToDouble(Double::valueOf)
                .average()
                .stream()
                .map(grade -> grade / 100.0)
                .boxed()
                .findFirst();
    }

    private Optional<Double> numberOfReviewsFrom(Element htmlWithScores) {
        double count = MetacriticStreamSelectors.CRITIC_REVIEWS.extractFrom(htmlWithScores)
                .map(Element::text)
                .mapToDouble(Double::valueOf)
                .count();
        return Optional.of(count).filter(Doubles.greaterThan(0.0));
    }

    private Optional<Score.ScoreBuilder> nytScoreFrom(Element htmlWithScores) {
        return MetacriticSelectors.NEW_YORK_TIMES_GRADE.extractFrom(htmlWithScores)
                .map(grade -> (Double.valueOf(grade) / 100.0))
                .map(percentage -> Score.builder()
                        .grade(percentage)
                        .quantity(NYT_SCORE_WEIGHT)
                        .source("New York Times")
                        .type(ScoreType.CRITIC));
    }

    private Optional<Element> getSearchResultsFor(Title title) {
        return getPage("search", "movie", title.asText(), "results");
    }

    private Optional<Element> getCriticScoresFor(String href) {
        return getPage(href, "critic-reviews");
    }

    private Optional<Element> getPage(String... pathSegments) {
        var builder = metacriticUrlBuilder();
        Arrays.stream(pathSegments).forEach(builder::addPathSegments);
        var url = builder.build();
        return connection.connectToAndGet(url);
    }

    private HttpUrl.Builder metacriticUrlBuilder() {
        return new HttpUrl.Builder()
                .scheme("http")
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
