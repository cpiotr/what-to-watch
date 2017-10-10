package pl.ciruk.core.stream;

import java.util.Optional;
import java.util.function.BiFunction;

public class Optionals {

    public static <T, U> Optional<U> mergeUsing(Optional<T> first, Optional<T> second, BiFunction<T, T, U> merger) {
        return first.flatMap(f -> second.map(s -> merger.apply(f, s)));
    }
}
