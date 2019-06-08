package pl.ciruk.whattowatch.utils.metrics;

import pl.ciruk.whattowatch.core.score.ScoresProvider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Names {

    private static final String MISSING_SCORES = Names.createName(ScoresProvider.class, List.of("missing", "count"));

    private Names() {
        throw new AssertionError();
    }

    public static <T> String createName(Class<?> clazz, T suffix) {
        return createName(clazz, List.of(suffix));
    }

    public static String createName(Class<?> clazz, Iterable<?> suffixElements) {
        Stream<String> stream = Stream.concat(
                Stream.of(clazz.getSimpleName()),
                StreamSupport.stream(suffixElements.spliterator(), false).map(Object::toString));

        return stream.collect(Collectors.joining("."));
    }

    public static String getNameForMissingScores() {
        return MISSING_SCORES;
    }
}
