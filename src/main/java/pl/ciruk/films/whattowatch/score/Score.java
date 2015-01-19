package pl.ciruk.films.whattowatch.score;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Score {

	private double grade;
	private int quantity;

	public Score(double grade, int quantity) {
		this.grade = grade;
		this.quantity = quantity;
	}
}
