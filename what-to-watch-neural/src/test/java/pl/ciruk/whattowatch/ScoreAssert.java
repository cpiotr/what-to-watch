package pl.ciruk.whattowatch;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.ciruk.whattowatch.score.Score;

import static org.assertj.core.api.Assertions.within;

public class ScoreAssert extends AbstractAssert<ScoreAssert, Score> {
    private ScoreAssert(Score actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public static ScoreAssert assertThat(Score score) {
        return new ScoreAssert(score, ScoreAssert.class);
    }

    public static boolean isMeaningful(Score item) {
        boolean validGrade = item.getGrade() >= 0.1 && item.getGrade() <= 1.0;
        boolean significant = item.isSignificant();

        return validGrade && significant;
    }

    public ScoreAssert isMeaningful() {
        Assertions.assertThat(actual.getGrade())
                .as("Grade between 10% and 100%")
                .isBetween(0.1, 1.0);

        Assertions.assertThat(actual.isSignificant())
                .as("Score is significant")
                .isTrue();

        return this;
    }

    public ScoreAssert hasGradeGreaterThan(double threshold) {
        Assertions.assertThat(actual.getGrade())
                .as("Grade between %.2f%% and 100%%", threshold*100)
                .isBetween(threshold, 1.0);
        return this;
    }

    public ScoreAssert hasGradeLessThan(double threshold) {
        Assertions.assertThat(actual.getGrade())
                .as("Grade between 0%% and %.0f%%", threshold*100)
                .isBetween(0.0, threshold);
        return this;
    }

    public ScoreAssert hasGradeCloseTo(double threshold) {
        Assertions.assertThat(actual.getGrade())
                .as("Grade close to %.2f%%", threshold * 100)
                .isCloseTo(threshold, within(0.5));
        return this;
    }
}
