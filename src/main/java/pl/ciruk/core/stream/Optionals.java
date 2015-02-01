package pl.ciruk.core.stream;

import java.util.Optional;
import java.util.stream.Stream;

public class Optionals {
    public static <T> Stream<T> asStream(Optional<T> optional) {
        return optional.isPresent()
                ? Stream.of(optional.get())
                : Stream.empty();
    }
}
