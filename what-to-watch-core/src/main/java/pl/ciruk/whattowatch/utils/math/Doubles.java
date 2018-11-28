package pl.ciruk.whattowatch.utils.math;

import java.util.function.Predicate;
@SuppressWarnings("PMD.ClassNamingConventions")

public final class Doubles {
    private static final double EPSILON = 1e-5;

    private Doubles() {
        throw new AssertionError();
    }

    public static Predicate<Double> greaterThan(double threshold) {
        return value -> Math.abs(value - threshold) > EPSILON;
    }

    public static double normalizeScore(double percentage, long totalQuantity) {
        return WilsonScore.confidenceIntervalLowerBound(percentage, totalQuantity, 0.02);
    }
}
