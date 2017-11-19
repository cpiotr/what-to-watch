package pl.ciruk.core.stream;

import lombok.experimental.UtilityClass;

import java.util.function.Predicate;

@UtilityClass
public final class Predicates {
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
}
