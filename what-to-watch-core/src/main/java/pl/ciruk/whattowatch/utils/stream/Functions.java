package pl.ciruk.whattowatch.utils.stream;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Functions {
    private Functions() {
        throw new AssertionError();
    }

    public static <T> UnaryOperator<T> identity(Runnable action) {
        return value -> {
            action.run();
            return value;
        };
    }

    public static <T> UnaryOperator<T> identity(Consumer<T> action) {
        return value -> {
            action.accept(value);
            return value;
        };
    }

    public static <T> Consumer<T> consumeNothing() {
        return __ -> {};
    }
}
