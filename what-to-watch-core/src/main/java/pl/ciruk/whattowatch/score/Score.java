package pl.ciruk.whattowatch.score;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Score {

    private double grade;
    private long quantity;

    public Score(double grade, long quantity) {
        this.grade = grade;
        this.quantity = Math.toIntExact(quantity);
    }
}
