package pl.ciruk.films.whattowatch.score;

import java.util.stream.Stream;

import pl.ciruk.films.whattowatch.description.Description;

public interface ScoresProvider {
	Stream<Score> scoresOf(Description description);
}
