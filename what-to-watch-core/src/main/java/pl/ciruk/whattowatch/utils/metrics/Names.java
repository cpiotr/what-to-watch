package pl.ciruk.whattowatch.utils.metrics;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Names {
    private Names() {
        throw new AssertionError();
    }

    public static String createName(Class<?> clazz) {
        return createName(clazz, List.of());
    }

    public static <T> String createName(Class<?> clazz, T tag) {
        return createName(clazz, List.of(tag));
    }

    public static String createName(Class<?> clazz, Iterable<?> tags) {
        Stream<String> stream = Stream.concat(
                Stream.of(clazz.getSimpleName()),
                Streams.stream(tags).map(Object::toString));

        return stream.collect(Collectors.joining("."));
    }
}
