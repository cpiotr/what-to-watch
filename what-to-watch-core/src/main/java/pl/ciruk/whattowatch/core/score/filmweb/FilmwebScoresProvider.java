package pl.ciruk.whattowatch.core.score.filmweb;

import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebFilm;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.text.NumberTokenizer;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class FilmwebScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FILMWEB = "Filmweb";

    private final FilmwebProxy filmwebProxy;
    private final ExecutorService executorService;
    private final AtomicLong missingScores = new AtomicLong();

    public FilmwebScoresProvider(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
        this.executorService = executorService;

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(FILMWEB)),
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

        var title = description.getTitle();
        var scores = filmwebProxy.searchBy(title.asText(), title.year())
                .filter(film -> film.toTitle().matches(title))
                .map(this::findScore)
                .flatMap(Optional::stream)
                .filter(Score::isSignificant)
                .toList();

        if (scores.isEmpty()) {
            LOGGER.warn("Missing Filmweb score for {}", description);
            missingScores.incrementAndGet();
        }
        scores.forEach(score -> LOGGER.debug("Score for {}: {}", description, score));
        return scores.stream();
    }

    private Optional<Score> findScore(FilmwebFilm film) {
        return filmwebProxy.getPageWithFilmDetailsFor(film)
                .flatMap(FilmwebSelectors.SCORE_FROM_DETAILS::extractFrom)
                .map(scoreText -> createScore(scoreText, filmwebProxy.linkTo(film)))
                .filter(score -> score.grade() > 0 && score.quantity() > 0);
    }

    private static Score createScore(String scoreText, HttpUrl link) {
        var numberTokenizer = new NumberTokenizer(scoreText);
        var rating = numberTokenizer.hasMoreTokens() ? numberTokenizer.nextToken().asNormalizedDouble() : -1;
        var quantity = numberTokenizer.hasMoreTokens() ? (int) numberTokenizer.nextToken().asSimpleLong() : -1;
        return Score.builder()
                .grade(rating / 10.0)
                .quantity(quantity)
                .source(FILMWEB)
                .type(ScoreType.AMATEUR)
                .url(link.toString())
                .build();
    }
}
