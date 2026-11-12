package pl.ciruk.whattowatch.utils.math;

final class WilsonScore {
    // The normal distribution's 95th percentile, for a two-sided 90% confidence interval
    private static final double Z_SCORE = 1.644_853_646_608_357;

    private WilsonScore() {
        throw new AssertionError();
    }

    static double confidenceIntervalLowerBound(double share, long total) {
        if (total == 0) {
            return 0.0;
        }
        long positive = (long) (share * total);
        double observed = (double) positive / total;
        double zSquared = Z_SCORE * Z_SCORE;
        return (observed + zSquared / (2 * total) - Z_SCORE * Math.sqrt((observed * (1 - observed) + zSquared / (4 * total)) / total))
                / (1 + zSquared / total);
    }
}
