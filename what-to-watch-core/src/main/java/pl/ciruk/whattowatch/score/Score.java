package pl.ciruk.whattowatch.score;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Score {

    private final String source;

    private final double grade;

    private final long quantity;

    public Score(double grade, long quantity, String source) {
        this.grade = grade;
        this.quantity = Math.toIntExact(quantity);
        this.source = source;
    }
}
