package pl.ciruk.whattowatch.score.filmweb;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.core.text.NumberTokenizer;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.source.FilmwebProxy;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

@Slf4j
public class FilmwebScores implements ScoresProvider {
    private final FilmwebProxy filmwebProxy;
    private final ExecutorService executorService;

    public FilmwebScores(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
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

        return scoresForTitle(description.titleAsText(), description.getYear())
                .peek(score -> log.debug("scoresOf - Score for {}: {}", description, score));
    }

    Stream<Score> scoresForTitle(String title, int year) {
        Optional<Element> optionalResult = filmwebProxy.searchFor(title, year);

        return Optionals.asStream(optionalResult)
                .flatMap(page -> FilmwebStreamSelectors.FILMS_FROM_SEARCH_RESULT.extractFrom(page))
                .map(FilmwebSelectors.SCORE::extractFrom)
                .flatMap(Optionals::asStream)
                .map(this::parseScore)
                .filter(this::isPositive);
    }

    private boolean isPositive(Score score) {
        return score.getGrade() > 0 && score.getQuantity() > 0;
    }

    private Score parseScore(String s) {
        NumberTokenizer numberTokenizer = new NumberTokenizer(s);
        double rating = numberTokenizer.hasMoreTokens() ? numberTokenizer.nextToken().asNormalizedDouble() : -1;
        int quantity = numberTokenizer.hasMoreTokens() ? (int) numberTokenizer.nextToken().asSimpleLong() : -1;
        return new Score(rating, quantity);
    }
}
