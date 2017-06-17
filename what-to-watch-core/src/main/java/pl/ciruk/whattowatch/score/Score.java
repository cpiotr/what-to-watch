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
        return Score.builder()
                .grade(grade)
                .quantity(ScoreType.AMATEUR.getWeight())
                .type(ScoreType.AMATEUR)
                .build();
    }

    public static Score critic(double grade) {
        return Score.builder()
                .grade(grade)
                .quantity(ScoreType.CRITIC.getWeight())
                .type(ScoreType.CRITIC)
                .build();
    }
}
