package pl.ciruk.whattowatch.utils.metrics;

import pl.ciruk.whattowatch.core.score.ScoresProvider;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Names {
    private static final String MISSING_SCORES = Names.createName(ScoresProvider.class, List.of("missing", "count"));

    private Names() {
        throw new AssertionError();
    }

    public static <T> String createName(Class<?> clazz, T suffix) {
        return createName(clazz, List.of(suffix));
    }

    public static String createName(Class<?> clazz, List<?> suffixElements) {
        return Stream.concat(
                Stream.of(clazz.getSimpleName()),
                suffixElements.stream().map(Object::toString)
        ).collect(joining("."));
    }

    public static String getNameForMissingScores() {
        return MISSING_SCORES;
    }
}
