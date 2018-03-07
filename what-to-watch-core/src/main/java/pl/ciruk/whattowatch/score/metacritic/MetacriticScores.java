package pl.ciruk.whattowatch.score.metacritic;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import pl.ciruk.core.math.Doubles;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static pl.ciruk.core.stream.Optionals.mergeUsing;
import static pl.ciruk.whattowatch.score.metacritic.MetacriticSelectors.LINK_TO_DETAILS;
import static pl.ciruk.whattowatch.title.Title.MISSING_YEAR;

@Slf4j
public class MetacriticScores implements ScoresProvider {
    private static final int NYT_SCORE_WEIGHT = 10;

    private final HttpConnection<Element> connection;

    private final ExecutorService executorService;

    private final AtomicLong missingMetacriticScores = new AtomicLong();

    private final AtomicLong missingNewYorkTimesScores = new AtomicLong();

    public MetacriticScores(
            HttpConnection<Element> connection,
            MetricRegistry metricRegistry,
            ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;

        metricRegistry.register(
                name(MetacriticScores.class, "missingMetacriticScores"),
                (Gauge<Long>) missingMetacriticScores::get
        );

        metricRegistry.register(
                name(MetacriticScores.class, "missingNewYorkTimesScores"),
                (Gauge<Long>) missingNewYorkTimesScores::get
        );
    }

    @Override
    public CompletableFuture<Stream<Score>> scoresOfAsync(Description description) {
        return CompletableFuture.supplyAsync(
                () -> scoresOf(description),
                executorService
        );
    }

    @Override
    public Stream<Score> scoresOf(Description description) {
        log.debug("scoresOf - Description: {}", description);

        Optional<String> linkToDetails = metacriticSummaryOf(description.getTitle())
                .flatMap(LINK_TO_DETAILS::extractFrom);
        Optional<Element> htmlWithScores = linkToDetails
                .flatMap(this::getCriticScoresFor)
                .map(this::extractCriticReviews)
                .or(() -> followDetailsLinkAndFindPageWithScores(linkToDetails));

        Optional<Score> metacriticScore = htmlWithScores.flatMap(this::extractScoreFrom);
        if (!metacriticScore.isPresent()) {
            log.warn("scoresOf - Missing Metacritic score for: {}", description.getTitle());
            missingMetacriticScores.incrementAndGet();
        }

        Optional<Score> nytScore = htmlWithScores.flatMap(this::nytScoreFrom);
        if (!nytScore.isPresent()) {
            log.warn("scoresOf - Missing NYT score for: {}", description.getTitle());
            missingNewYorkTimesScores.incrementAndGet();
        }

        Stream<Score> averageScoreStream = metacriticScore.stream();
        Stream<Score> nytScoreStream = nytScore.stream();
        return Stream.concat(averageScoreStream, nytScoreStream)
                .peek(score -> log.debug("scoresOf - Score for {}: {}", description, score));
    }

    private Optional<Element> followDetailsLinkAndFindPageWithScores(Optional<String> linkToDetails) {
        return linkToDetails.flatMap(this::getPage)
                .flatMap(MetacriticSelectors.LINK_TO_CRITIC_REVIEWS::extractFrom)
                .flatMap(this::getPage)
                .map(this::extractCriticReviews);
    }

    private Element extractCriticReviews(Element page) {
        return page.select("#main_content .critic_reviews").first();
    }

    private Optional<Score> extractScoreFrom(Element htmlWithScores) {
        Optional<Double> averageGrade = averageGradeFrom(htmlWithScores);
        Optional<Double> numberOfReviews = numberOfReviewsFrom(htmlWithScores);
        return mergeUsing(
                averageGrade,
                numberOfReviews,
                this::createScore);
    }

    private Score createScore(Double rating, Double count) {
        return Score.builder()
                .grade(rating)
                .quantity(count.intValue())
                .source("Metacritic")
                .type(ScoreType.CRITIC)
                .build();
    }

    private Optional<Double> averageGradeFrom(Element htmlWithScores) {
        return MetacriticStreamSelectors.CRITIC_REVIEWS.extractFrom(htmlWithScores)
                .map(Element::text)
                .mapToDouble(Double::valueOf)
                .average()
                .stream()
                .boxed()
                .findFirst();
    }

    private Optional<Double> numberOfReviewsFrom(Element htmlWithScores) {
        double count = MetacriticStreamSelectors.CRITIC_REVIEWS.extractFrom(htmlWithScores)
                .map(Element::text)
                .mapToDouble(Double::valueOf)
                .count();
        return Optional.of(count).filter(Doubles.isGreaterThan(0.0));
    }

    private Optional<Score> nytScoreFrom(Element htmlWithScores) {
        return MetacriticSelectors.NEW_YORK_TIMES_GRADE.extractFrom(htmlWithScores)
                .map(grade -> (Double.valueOf(grade) / 100.0))
                .map(percentage -> Score.builder()
                        .grade(percentage)
                        .quantity(NYT_SCORE_WEIGHT)
                        .source("New York Times")
                        .type(ScoreType.CRITIC)
                        .build());
    }

    private Optional<Element> getSearchResultsFor(Title title) {
        return getPage("search", "movie", title.asText(), "results");
    }

    private Optional<Element> getCriticScoresFor(String href) {
        return getPage(href, "critic-reviews");
    }

    private Optional<Element> getPage(String... pathSegments) {
        HttpUrl.Builder builder = metacriticUrlBuilder();
        Arrays.stream(pathSegments).forEach(builder::addPathSegments);
        HttpUrl url = builder.build();
        return connection.connectToAndGet(url.toString());
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
            log.warn("Cannot find metacritic summary of {}", title, e);
            return Optional.empty();
        }
    }

    private Optional<Element> findFirstResultMatching(Title title, Element searchResults) {
        return MetacriticStreamSelectors.SEARCH_RESULTS.extractFrom(searchResults)
                .filter(e -> extractTitle(e).matches(title))
                .findFirst();
    }

    private Title extractTitle(Element searchResult) {
        String title = MetacriticSelectors.TITLE.extractFrom(searchResult).orElse("");
        Integer year = MetacriticSelectors.RELEASE_DATE.extractFrom(searchResult)
                .map(Integer::parseInt)
                .orElse(MISSING_YEAR);

        return Title.builder()
                .title(title)
                .year(year)
                .build();
    }
}
