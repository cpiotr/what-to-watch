package pl.ciruk.core.math;

import java.util.function.Predicate;

public final class Doubles {

    public static final double EPSILON = 1e-5;

    public static Predicate<Double> isGreaterThan(double threshold) {
        return value -> Math.abs(value - threshold) > EPSILON;
    }

    private Doubles() {
        throw new AssertionError();
    }
}
