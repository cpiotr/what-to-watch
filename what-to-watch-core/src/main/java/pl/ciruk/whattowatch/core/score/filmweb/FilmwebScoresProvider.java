package pl.ciruk.whattowatch.core.score.filmweb;

import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.GenericScoreProvider;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class FilmwebScoresProvider implements ScoresProvider {
    private final GenericScoreProvider genericScoreProvider;

    public FilmwebScoresProvider(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        genericScoreProvider = new GenericScoreProvider(executorService, new FilmwebScoresFinder(filmwebProxy));
    }

    @Override
    public CompletableFuture<Stream<Score>> findScoresByAsync(Description description) {
        return genericScoreProvider.findScoresByAsync(description);
    }

    @Override
    public Stream<Score> findScoresBy(Description description) {
        return genericScoreProvider.findScoresBy(description)
                .filter(Score::isSignificant);
    }
}
