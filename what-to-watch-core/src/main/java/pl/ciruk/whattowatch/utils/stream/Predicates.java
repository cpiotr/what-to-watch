package pl.ciruk.whattowatch.utils.stream;

import java.util.function.Predicate;

public final class Predicates {
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
}
