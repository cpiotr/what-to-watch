package pl.ciruk.whattowatch.score;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class Score {

    private final double grade;

    private final long quantity;

    private String source;

    private ScoreType type;

    public static Score amateur(double grade) {
        return amateur(grade, ScoreType.AMATEUR.getWeight());
    }

    public static Score amateur(double grade, long quantity) {
        return Score.builder()
                .grade(grade)
                .quantity(quantity)
                .type(ScoreType.AMATEUR)
                .build();
    }

    public static Score critic(double grade) {
        return critic(grade, ScoreType.CRITIC.getWeight());
    }

    public static Score critic(double grade, long quantity) {
        return Score.builder()
                .grade(grade)
                .quantity(quantity)
                .type(ScoreType.CRITIC)
                .build();
    }
}
