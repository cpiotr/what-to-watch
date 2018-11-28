package pl.ciruk.whattowatch.utils.stream;

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
}
