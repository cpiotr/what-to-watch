package pl.ciruk.whattowatch.score.imdb;

import com.squareup.okhttp.HttpUrl;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.core.text.NumberToken;
import pl.ciruk.core.text.NumberTokenizer;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    public ImdbWebScores(HttpConnection<Element> httpConnection, ExecutorService executorService) {
        this.httpConnection = httpConnection;
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
        log.info("scoresOf - Description: {}", description);

        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host("www.imdb.com")
                .addPathSegment("search")
                .addPathSegment("title")
                .addQueryParameter("title", description.titleAsText())
                .addQueryParameter("release_date", String.valueOf(description.getYear()))
                .build();

        Optional<Score> firstResult = Optionals.asStream(httpConnection.connectToAndGet(url.toString()))
                .flatMap(FILMS_FROM_SEARCH_RESULT::extractFrom)
                .filter(result -> TITLE.extractFrom(result).filter(matchesTitleFrom(description)).isPresent())
                .filter(result -> extractYearFrom(result).filter(matchesYearFrom(description)).isPresent())
                .findFirst()
                .flatMap(this::extractScore);
        if (!firstResult.isPresent()) {
            log.warn("scoresOf - Missing score for {}; Search query: {}", description, url.toString());
        }

        return Optionals.asStream(firstResult)
                .peek(score -> log.debug("scoresOf - Score for {}: {}", description, score));
    }

    private Predicate<Integer> matchesYearFrom(Description description) {
        return year -> year.equals(description.getYear());
    }

    private Predicate<String> matchesTitleFrom(Description description) {
        return title -> description.getTitle().getOriginalTitle().equalsIgnoreCase(title)
                || description.getTitle().getTitle().equalsIgnoreCase(title);
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

        return Optional.of(new Score(asPercentage(grade), quantity))
                .filter(score -> score.getGrade() > 0.0)
                .filter(score -> score.getQuantity() > 0);
    }

    private double asPercentage(double imdbRating) {
        return imdbRating / MAX_IMDB_SCORE;
    }
}
