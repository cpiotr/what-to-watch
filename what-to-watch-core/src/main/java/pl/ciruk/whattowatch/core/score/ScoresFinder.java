package pl.ciruk.whattowatch.core.score;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.core.description.Description;

import java.util.Optional;
import java.util.stream.Stream;

public interface ScoresFinder {
    Stream<Element> searchFor(Description description);

    boolean matchesDescription(Element search, Description description);

    Optional<Score> extractScore(Element scoreResult);

    String getName();
}
