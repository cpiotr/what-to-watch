package pl.ciruk.whattowatch.core.score;

import pl.ciruk.whattowatch.core.description.Description;

import java.util.Optional;
import java.util.stream.Stream;

public interface ScoresFinder<T> {
    Stream<T> searchFor(Description description);

    boolean matchesDescription(T searchResult, Description description);

    Optional<Score> extractScore(T searchResult);

    String getName();
}
