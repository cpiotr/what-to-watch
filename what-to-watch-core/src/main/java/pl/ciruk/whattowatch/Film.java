package pl.ciruk.whattowatch;

import lombok.Builder;
import lombok.Data;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;

import java.util.List;

import static pl.ciruk.core.math.WilsonScore.confidenceIntervalLowerBound;

@Builder
@Data
public class Film {
	static final Film EMPTY = Film.builder().build();

	Description description;

	List<Score> scores;

	public static Film empty() {
		return EMPTY;
	}

	public boolean isWorthWatching() {
		return isNotEmpty()
				&& normalizedScore() > 0.65
				&& scores.size() > 2;
	}

	public Double normalizedScore() {
		long totalQuantity = scores.stream()
				.mapToLong(Score::getQuantity)
				.sum();
		int positiveQuantity = (int) (score() * totalQuantity);
		return confidenceIntervalLowerBound(positiveQuantity, totalQuantity, 0.95);
	}

	public double score() {
		return scores.stream()
				.mapToDouble(Score::getGrade)
				.average()
				.orElse(0.0);
	}

	public boolean isNotEmpty() {
		return this != EMPTY;
	}
}
