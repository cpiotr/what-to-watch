package pl.ciruk.whattowatch.score.imdb;

import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.text.NumberToken;
import pl.ciruk.core.text.NumberTokenizer;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.score.imdb.ImdbSelectors.*;
import static pl.ciruk.whattowatch.score.imdb.ImdbStreamSelectors.FILMS_FROM_SEARCH_RESULT;

public class ImdbWebScores implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_IMDB_SCORE = 10;

    private final HttpConnection<Element> httpConnection;

    private final ExecutorService executorService;

    private final AtomicLong missingScores = new AtomicLong();

    public ImdbWebScores(
            HttpConnection<Element> httpConnection,
            ExecutorService executorService) {
        this.httpConnection = httpConnection;
        this.executorService = executorService;

        Metrics.gauge(
                MethodHandles.lookup().lookupClass().getSimpleName() + "missingScores",
                missingScores,
                AtomicLong::get
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
        LOGGER.debug("scoresOf - Description: {}", description);

        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host("www.imdb.com")
                .addPathSegment("search")
                .addPathSegment("title")
                .addQueryParameter("title", description.titleAsText())
                .addQueryParameter("release_date", String.valueOf(description.getYear()))
                .addQueryParameter("title_type", "feature,tv_movie")
                .build();

        Optional<Score> firstResult = httpConnection.connectToAndGet(url.toString())
                .flatMap(searchResults -> findFirstResult(searchResults, description))
                .flatMap(this::extractScore);
        if (!firstResult.isPresent()) {
            LOGGER.warn("scoresOf - Missing score for {}", description);
            LOGGER.trace("scoresOf - Search query: {}", url.toString());
            missingScores.incrementAndGet();
        }

        return firstResult.stream()
                .peek(score -> LOGGER.debug("scoresOf - Score for {}: {}", description, score));
    }

    private Optional<Element> findFirstResult(Element searchResults, Description description) {
        return FILMS_FROM_SEARCH_RESULT.extractFrom(searchResults)
                .filter(searchResult -> matchesTitleFromDescription(searchResult, description))
                .findAny();
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
