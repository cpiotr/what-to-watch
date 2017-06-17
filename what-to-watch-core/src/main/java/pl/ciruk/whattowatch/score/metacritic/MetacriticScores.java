package pl.ciruk.whattowatch.score.metacritic;

import com.squareup.okhttp.HttpUrl;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static pl.ciruk.core.stream.Optionals.mergeUsing;
import static pl.ciruk.whattowatch.score.metacritic.MetacriticSelectors.LINK_TO_DETAILS;
import static pl.ciruk.whattowatch.title.Title.MISSING_YEAR;

@Slf4j
public class MetacriticScores implements ScoresProvider {
    private static final String METACRITIC_BASE_URL = "http://www.metacritic.com";

    private static final int NYT_SCORE_WEIGHT = 10;

    private final HttpConnection<Element> connection;

    private final ExecutorService executorService;

    public MetacriticScores(HttpConnection<Element> connection, ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;
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

        if (description.titleAsText().contains("T2")) {
            System.out.println();
        }

        Optional<Element> htmlWithScores = metacriticSummaryOf(description.getTitle())
                .flatMap(LINK_TO_DETAILS::extractFrom)
                .flatMap(href -> downloadPage(METACRITIC_BASE_URL + href))
                .flatMap(MetacriticSelectors.LINK_TO_CRITIC_REVIEWS::extractFrom)
                .flatMap(href -> downloadPage(METACRITIC_BASE_URL + href))
                .map(page -> page.select("#main_content").first());

        Optional<Score> metacriticScore = htmlWithScores.flatMap(this::extractScoreFrom);
        if (!metacriticScore.isPresent()) {
            log.warn("scoresOf - Missing Metacritic score for: {}", description.getTitle());
        }

        Optional<Score> nytScore = htmlWithScores.flatMap(this::nytScoreFrom);
        if (!nytScore.isPresent()) {
            log.warn("scoresOf - Missing NYT score for: {}", description.getTitle());
        }

        Stream<Score> averageScoreStream = Optionals.asStream(metacriticScore);
        Stream<Score> nytScoreStream = Optionals.asStream(nytScore);
        return Stream.concat(averageScoreStream, nytScoreStream)
                .peek(score -> log.debug("scoresOf - Score for {}: {}", description, score));
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
        return MetacriticSelectors.AVERAGE_GRADE.extractFrom(htmlWithScores)
                .map(Double::valueOf)
                .map(d -> d / 100.0);
    }

    private Optional<Double> numberOfReviewsFrom(Element htmlWithScores) {
        return MetacriticSelectors.NUMBER_OF_GRADES.extractFrom(htmlWithScores)
                .map(Double::valueOf);
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

    private Optional<Element> downloadPage(String url) {
        return connection.connectToAndGet(url);
    }

    private Optional<Element> metacriticSummaryOf(Title title) {
        try {
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host("www.metacritic.com")
                    .addPathSegment("search")
                    .addPathSegment("movie")
                    .addPathSegment(title.asText())
                    .addPathSegment("results")
                    .build();

            return downloadPage(url.toString())
                    .flatMap(page -> MetacriticStreamSelectors.SEARCH_RESULTS.extractFrom(page)
                            .filter(e -> extractTitle(e).matches(title))
                            .findFirst()
                    );
        } catch (Exception e) {
            log.warn("Cannot find metacritic summary of {}", title, e);
            return Optional.empty();
        }
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
