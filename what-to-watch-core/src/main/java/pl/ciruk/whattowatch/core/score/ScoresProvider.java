package pl.ciruk.whattowatch.core.score;

import pl.ciruk.whattowatch.core.description.Description;

import java.util.stream.Stream;

public interface ScoresProvider {
    Stream<Score> findScoresBy(Description description);
}
