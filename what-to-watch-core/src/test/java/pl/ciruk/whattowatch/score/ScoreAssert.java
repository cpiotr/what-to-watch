package pl.ciruk.whattowatch.score;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

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
}
