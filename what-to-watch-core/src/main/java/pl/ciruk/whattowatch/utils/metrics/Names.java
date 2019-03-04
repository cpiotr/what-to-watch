package pl.ciruk.whattowatch.utils.metrics;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Names {
    private Names() {
        throw new AssertionError();
    }

    public static <T> String createName(Class<?> clazz, T tag) {
        return createName(clazz, List.of(tag));
    }

    public static String createName(Class<?> clazz, Iterable<?> tags) {
        Stream<String> stream = Stream.concat(
                Stream.of(clazz.getSimpleName()),
                StreamSupport.stream(tags.spliterator(), false).map(Object::toString));

        return stream.collect(Collectors.joining("."));
    }
}
