package pl.ciruk.whattowatch.suggest;

import lombok.Builder;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;

import java.util.List;

@Builder
public class Film {
	static final Film EMPTY = Film.builder().build();

	Description description;

	List<Score> scores;

	public static Film empty() {
		return EMPTY;
	}
}
