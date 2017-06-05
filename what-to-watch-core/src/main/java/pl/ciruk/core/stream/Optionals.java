package pl.ciruk.core.stream;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class Optionals {
    public static <T> Stream<T> asStream(Optional<T> optional) {
        return optional.map(Stream::of).orElseGet(Stream::empty);
    }

    public static <T, U> Optional<U> mergeUsing(Optional<T> first, Optional<T> second, BiFunction<T, T, U> merger) {
        return first.flatMap(f -> second.map(s -> merger.apply(f, s)));
    }
}
