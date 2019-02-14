package pl.ciruk.whattowatch.core.score.filmweb;

import io.micrometer.core.instrument.Metrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.text.NumberTokenizer;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

public class FilmwebScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmwebProxy filmwebProxy;

    private final ExecutorService executorService;

    private final AtomicLong missingScores = new AtomicLong();

    public FilmwebScoresProvider(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
        this.executorService = executorService;

        Metrics.gauge(
                Names.createName(ScoresProvider.class, List.of("filmweb", "missing", "count")),
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
        LOGGER.debug("Description: {}", description);

        return scoresForTitle(description.getTitle())
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score))
                .filter(Score::isSignificant);
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
                .map(scoreText -> createScore(scoreText, linkToDetails));
    }

    private boolean isPositive(Score score) {
        return score.getGrade() > 0 && score.getQuantity() > 0;
    }

    private Score createScore(String scoreText, String link) {
        var numberTokenizer = new NumberTokenizer(scoreText);
        var rating = numberTokenizer.hasMoreTokens() ? numberTokenizer.nextToken().asNormalizedDouble() : -1;
        var quantity = numberTokenizer.hasMoreTokens() ? (int) numberTokenizer.nextToken().asSimpleLong() : -1;
        return Score.builder()
                .grade(rating/10.0)
                .quantity(quantity)
                .source("Filmweb")
                .type(ScoreType.AMATEUR)
                .url(filmwebProxy.resolveLink(link).toString())
                .build();
    }

    private Consumer<Optional<?>> logIfMissing(Title title) {
        return score -> {
            if (score.isEmpty()) {
                LOGGER.warn("Missing score for: {}", title);
                missingScores.incrementAndGet();
            }
        };
    }
}
