package pl.ciruk.whattowatch.core.score;

import pl.ciruk.whattowatch.core.description.Description;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface ScoresProvider {
    Stream<Score> findScoresBy(Description description);

    CompletableFuture<Stream<Score>> findScoresByAsync(Description description);
}
