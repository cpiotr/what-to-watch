package pl.ciruk.whattowatch.utils.stream;

import java.util.Optional;
import java.util.function.BiFunction;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Optionals {
    private Optionals() {
        throw new AssertionError();
    }

    public static <T, U> Optional<U> mergeUsing(Optional<T> first, Optional<T> second, BiFunction<T, T, U> merger) {
        return first.flatMap(f -> second.map(s -> merger.apply(f, s)));
    }
}
