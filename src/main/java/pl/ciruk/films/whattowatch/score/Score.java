package pl.ciruk.films.whattowatch.score;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Score {

	private double score;
	private int quantity;

	public Score(double nextDouble, int nextInt) {
		this.score = nextDouble;
		this.quantity = nextInt;
	}
}
