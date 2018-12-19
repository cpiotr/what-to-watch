package pl.ciruk.whattowatch.core.score.imdb;

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
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.text.NumberToken;
import pl.ciruk.whattowatch.utils.text.NumberTokenizer;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.core.score.imdb.ImdbSelectors.*;
import static pl.ciruk.whattowatch.core.score.imdb.ImdbStreamSelectors.FILMS_FROM_SEARCH_RESULT;

public class ImdbScores implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_IMDB_SCORE = 10;

    private final HttpConnection<Element> httpConnection;

    private final ExecutorService executorService;

    private final AtomicLong missingScores = new AtomicLong();

    public ImdbScores(
            HttpConnection<Element> httpConnection,
            ExecutorService executorService) {
        this.httpConnection = httpConnection;
        this.executorService = executorService;

        Metrics.gauge(
                Names.createName(ScoresProvider.class, List.of("imdb", "missing", "count")),
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

        var url = createUrlBuilder()
                .addPathSegment("search")
                .addPathSegment("title")
                .addQueryParameter("title", description.titleAsText())
                .addQueryParameter("release_date", String.valueOf(description.getYear()))
                .addQueryParameter("title_type", "feature,tv_movie,documentary")
                .build();

        var firstResult = httpConnection.connectToAndGet(url.toString())
                .flatMap(searchResults -> findFirstResult(searchResults, description))
                .flatMap(this::extractScore);
        if (firstResult.isEmpty()) {
            LOGGER.warn("Missing score for {}", description);
            LOGGER.trace("Search query: {}", url.toString());
            missingScores.incrementAndGet();
        }

        return firstResult.stream()
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score));
    }

    private static HttpUrl.Builder createUrlBuilder() {
        return new HttpUrl.Builder()
                .scheme("http")
                .host("www.imdb.com");
    }

    private Optional<Element> findFirstResult(Element searchResults, Description description) {
        return FILMS_FROM_SEARCH_RESULT.extractFrom(searchResults)
                .filter(searchResult -> matchesTitleFromDescription(searchResult, description))
                .findAny();
    }

    private boolean matchesTitleFromDescription(Element searchResult, Description description) {
        var descriptionTitle = description.getTitle();
        return extractTitleFrom(searchResult).matches(descriptionTitle)
                || extractFullTitleFrom(searchResult).matches(descriptionTitle);
    }

    private Title extractTitleFrom(Element searchResult) {
        return extractTitleBuilderFrom(searchResult)
                .build();
    }

    private Title extractFullTitleFrom(Element searchResult) {
        return extractTitleBuilderFrom(searchResult)
                .originalTitle(getOriginalTitle(searchResult).orElse(""))
                .build();
    }

    private Title.TitleBuilder extractTitleBuilderFrom(Element searchResult) {
        return Title.builder()
                .title(TITLE.extractFrom(searchResult).orElse(""))
                .year(extractYearFrom(searchResult).orElse(Title.MISSING_YEAR));
    }

    private Optional<String> getOriginalTitle(Element searchResult) {
        return ImdbSelectors.LINK_FROM_SEARCH_RESULT.extractFrom(searchResult)
                .flatMap(this::getDetails)
                .flatMap(ImdbSelectors.ORIGINAL_TITLE::extractFrom);
    }

    private Optional<Element> getDetails(String linkToDetails) {
        var url = createUrlBuilder().build().resolve(linkToDetails);

        return httpConnection.connectToAndGet(url);
    }

    private Optional<Integer> extractYearFrom(Element searchResult) {
        return YEAR.extractFrom(searchResult).map(Integer::parseInt);
    }

    private Optional<Score> extractScore(Element searchResult) {
        var grade = SCORE.extractFrom(searchResult)
                .map(NumberTokenizer::new)
                .filter(NumberTokenizer::hasMoreTokens)
                .map(NumberTokenizer::nextToken)
                .map(NumberToken::asNormalizedDouble)
                .orElse(-1.0);
        var quantity = NUMBER_OF_SCORES.extractFrom(searchResult)
                .map(NumberTokenizer::new)
                .filter(NumberTokenizer::hasMoreTokens)
                .map(NumberTokenizer::nextToken)
                .map(NumberToken::asSimpleLong)
                .orElse(-1L);

        var imdbScore = Score.builder()
                .grade(asPercentage(grade))
                .quantity(quantity)
                .source("IMDb")
                .type(ScoreType.AMATEUR)
                .url(extractLink(searchResult))
                .build();
        return Optional.of(imdbScore)
                .filter(score -> score.getGrade() > 0.0)
                .filter(score -> score.getQuantity() > 0);
    }

    private String extractLink(Element searchResult) {
        return ImdbSelectors.LINK_FROM_SEARCH_RESULT.extractFrom(searchResult)
                .map(link -> createUrlBuilder().build().resolve(link))
                .map(HttpUrl::toString)
                .orElse(null);
    }

    private double asPercentage(double imdbRating) {
        return imdbRating / MAX_IMDB_SCORE;
    }
}
