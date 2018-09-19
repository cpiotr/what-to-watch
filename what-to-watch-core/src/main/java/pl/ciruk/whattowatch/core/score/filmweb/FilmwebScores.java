package pl.ciruk.whattowatch.core.score.filmweb;

import io.micrometer.core.instrument.Metrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.text.NumberTokenizer;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

public class FilmwebScores implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmwebProxy filmwebProxy;

    private final ExecutorService executorService;

    private final AtomicLong missingScores = new AtomicLong();

    public FilmwebScores(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
        this.executorService = executorService;

        Metrics.gauge(
                FilmwebScores.class.getSimpleName() + ".missingScores",
                Collections.emptyList(),
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
        LOGGER.debug("findScoresBy - Description: {}", description);

        return scoresForTitle(description.getTitle())
                .peek(score -> LOGGER.debug("findScoresBy - Score for {}: {}", description, score));
    }

    private Stream<Score> scoresForTitle(Title title) {
        var optionalResult = filmwebProxy.searchFor(title.asText(), title.getYear());

        return optionalResult.stream()
                .flatMap(FilmwebStreamSelectors.FILMS_FROM_SEARCH_RESULT::extractFrom)
                .filter(result -> extractTitle(result).matches(title))
                .map(this::getDetailsAndFindScore)
                .peek(logIfMissing(title))
                .flatMap(Optional::stream);
    }

    private Title extractTitle(Element result) {
        var title = FilmwebSelectors.TITLE_FROM_SEARCH_RESULT.extractFrom(result).orElse("");
        var year = FilmwebSelectors.YEAR_FROM_SEARCH_RESULT.extractFrom(result)
                .filter(not(String::isEmpty))
                .map(Integer::parseInt)
                .orElse(0);
        return Title.builder()
                .title(title)
                .year(year)
                .build();
    }

    private Optional<Score> getDetailsAndFindScore(Element result) {
        return FilmwebSelectors.LINK_FROM_SEARCH_RESULT.extractFrom(result)
                .flatMap(this::findScoreInDetailsPage)
                .filter(this::isPositive);
    }

    private Optional<Score> findScoreInDetailsPage(String linkToDetails) {
        return filmwebProxy.getPageWithFilmDetailsFor(linkToDetails)
                .flatMap(FilmwebSelectors.SCORE_FROM_DETAILS::extractFrom)
                .map(this::parseScore);
    }

    private boolean isPositive(Score score) {
        return score.getGrade() > 0 && score.getQuantity() > 0;
    }

    private Score parseScore(String s) {
        var numberTokenizer = new NumberTokenizer(s);
        var rating = numberTokenizer.hasMoreTokens() ? numberTokenizer.nextToken().asNormalizedDouble() : -1;
        var quantity = numberTokenizer.hasMoreTokens() ? (int) numberTokenizer.nextToken().asSimpleLong() : -1;
        return Score.builder()
                .grade(rating/10.0)
                .quantity(quantity)
                .source("Filmweb")
                .type(ScoreType.AMATEUR)
                .build();
    }

    private Consumer<Optional<?>> logIfMissing(Title title) {
        return score -> {
            if (!score.isPresent()) {
                LOGGER.warn("Missing score for: {}", title);
                missingScores.incrementAndGet();
            }
        };
    }
}
