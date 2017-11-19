package pl.ciruk.core.math;

import lombok.experimental.UtilityClass;

import java.util.function.Predicate;

@UtilityClass
public final class Doubles {

    public static final double EPSILON = 1e-5;

    public static Predicate<Double> isGreaterThan(double threshold) {
        return value -> Math.abs(value - threshold) > EPSILON;
    }

    public static double normalizeScore(double percentage, long totalQuantity) {
        return WilsonScore.confidenceIntervalLowerBound(percentage, totalQuantity, 0.5);
    }
}
