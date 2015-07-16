package pl.ciruk.whattowatch.score;

import pl.ciruk.whattowatch.description.Description;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface ScoresProvider {
	Stream<Score> scoresOf(Description description);

	default CompletableFuture<Stream<Score>> scoresOfAsync(Description description) {
		return CompletableFuture.supplyAsync(
				() -> scoresOf(description)
		);
	}
}
