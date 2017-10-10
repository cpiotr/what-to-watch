package pl.ciruk.whattowatch.score.imdb;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.text.NumberToken;
import pl.ciruk.core.text.NumberTokenizer;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static pl.ciruk.whattowatch.score.imdb.ImdbSelectors.NUMBER_OF_SCORES;
import static pl.ciruk.whattowatch.score.imdb.ImdbSelectors.SCORE;
import static pl.ciruk.whattowatch.score.imdb.ImdbSelectors.TITLE;
import static pl.ciruk.whattowatch.score.imdb.ImdbSelectors.YEAR;
import static pl.ciruk.whattowatch.score.imdb.ImdbStreamSelectors.FILMS_FROM_SEARCH_RESULT;

@Slf4j
public class ImdbWebScores implements ScoresProvider {
    private static final int MAX_IMDB_SCORE = 10;

    private final HttpConnection<Element> httpConnection;

    private final ExecutorService executorService;

    private final AtomicLong missingScores = new AtomicLong();

    public ImdbWebScores(
            HttpConnection<Element> httpConnection,
            MetricRegistry metricRegistry,
            ExecutorService executorService) {
        this.httpConnection = httpConnection;
        this.executorService = executorService;

        metricRegistry.register(
                name(ImdbWebScores.class, "missingScores"),
                (Gauge<Long>) missingScores::get
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

        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host("www.imdb.com")
                .addPathSegment("search")
                .addPathSegment("title")
                .addQueryParameter("title", description.titleAsText())
                .addQueryParameter("release_date", String.valueOf(description.getYear()))
                .addQueryParameter("title_type", "feature,tv_movie")
                .build();

        Optional<Score> firstResult = httpConnection.connectToAndGet(url.toString()).stream()
                .flatMap(FILMS_FROM_SEARCH_RESULT::extractFrom)
                .filter(result -> matchesTitleFromDescription(result, description))
                .findFirst()
                .flatMap(this::extractScore);
        if (!firstResult.isPresent()) {
            log.warn("scoresOf - Missing score for {}", description);
            log.trace("scoresOf - Search query: {}", url.toString());
            missingScores.incrementAndGet();
        }

        return firstResult.stream()
                .peek(score -> log.debug("scoresOf - Score for {}: {}", description, score));
    }

    private boolean matchesTitleFromDescription(Element result, Description description) {
        Title descriptionTitle = description.getTitle();
        return extractTitleFrom(result).matches(descriptionTitle)
                || extractFullTitleFrom(result).matches(descriptionTitle);
    }

    private Title extractTitleFrom(Element result) {
        return Title.builder()
                .title(TITLE.extractFrom(result).orElse(""))
                .year(extractYearFrom(result).orElse(Title.MISSING_YEAR))
                .build();
    }

    private Title extractFullTitleFrom(Element result) {
        return Title.builder()
                .title(TITLE.extractFrom(result).orElse(""))
                .originalTitle(getOriginalTitle(result).orElse(""))
                .year(extractYearFrom(result).orElse(Title.MISSING_YEAR))
                .build();
    }

    private Optional<String> getOriginalTitle(Element result) {
        return ImdbSelectors.LINK_FROM_SEARCH_RESULT.extractFrom(result)
                .flatMap(this::getDetails)
                .flatMap(ImdbSelectors.ORIGINAL_TITLE::extractFrom);
    }

    private Optional<Element> getDetails(String linkToDetails) {
        String url = "http://www.imdb.com/" + linkToDetails;

        return httpConnection.connectToAndGet(url);
    }

    private Optional<Integer> extractYearFrom(Element result) {
        return YEAR.extractFrom(result).map(Integer::parseInt);
    }

    private Optional<Score> extractScore(Element element) {
        double grade = SCORE.extractFrom(element)
                .map(NumberTokenizer::new)
                .filter(NumberTokenizer::hasMoreTokens)
                .map(NumberTokenizer::nextToken)
                .map(NumberToken::asNormalizedDouble)
                .orElse(-1.0);
        long quantity = NUMBER_OF_SCORES.extractFrom(element)
                .map(NumberTokenizer::new)
                .filter(NumberTokenizer::hasMoreTokens)
                .map(NumberTokenizer::nextToken)
                .map(NumberToken::asSimpleLong)
                .orElse(-1L);

        Score imdbScore = Score.builder()
                .grade(asPercentage(grade))
                .quantity(quantity)
                .source("IMDb")
                .type(ScoreType.AMATEUR)
                .build();
        return Optional.of(imdbScore)
                .filter(score -> score.getGrade() > 0.0)
                .filter(score -> score.getQuantity() > 0);
    }

    private double asPercentage(double imdbRating) {
        return imdbRating / MAX_IMDB_SCORE;
    }
}
