package pl.ciruk.whattowatch.utils.stream;

import java.util.function.Predicate;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Predicates {
    private Predicates() {
        throw new AssertionError();
    }

    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
}
