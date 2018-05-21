package pl.ciruk.core.stream;

import java.util.function.UnaryOperator;

public class Functions {
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
