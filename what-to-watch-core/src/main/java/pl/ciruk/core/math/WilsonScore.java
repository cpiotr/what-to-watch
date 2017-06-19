package pl.ciruk.core.math;


import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class WilsonScore {
    private static double P_NORMAL_DISTRIBUTION(double qn) {
        double[] b = {
                1.570796288, 0.03706987906, -0.8364353589e-3,
                -0.2250947176e-3, 0.6841218299e-5, 0.5824238515e-5,
                -0.104527497e-5, 0.8360937017e-7, -0.3231081277e-8,
                0.3657763036e-10, 0.6936233982e-12
        };

        if (qn < 0.0 || 1.0 < qn) {
            return 0.0;
        }

        if (qn == 0.5) {
            return 0.0;
        }

        double w1 = qn;
        if (qn > 0.5) {
            w1 = 1.0 - w1;
        }

        double w3 = -log(4.0 * w1 * (1.0 - w1));
        w1 = b[0];
        int i = 1;
        for (; i < 11; i++) {
            w1 += b[i] * pow(w3, i);
        }

        if (qn > 0.5) {
            return sqrt(w1 * w3);
        }

        return -sqrt(w1 * w3);
    }

    static Double confidenceIntervalLowerBound(long pos, long n, double power) {
        if (n == 0)
            return 0.0;
        double z = P_NORMAL_DISTRIBUTION(1 - power / 2);
        double phat = 1.0 * pos / n;
        return (phat + z * z / (2 * n) - z * sqrt((phat * (1 - phat) + z * z / (4 * n)) / n)) / (1 + z * z / n);
    }

    static Double confidenceIntervalLowerBound(double percentage, long n, double power) {
        long pos = (long) (percentage * n);
        return confidenceIntervalLowerBound(pos, n, power);
    }
}
