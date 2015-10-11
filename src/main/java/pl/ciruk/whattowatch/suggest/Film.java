package pl.ciruk.whattowatch.suggest;

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
		return normalizedScore() > 0.7
				&& scores.size() > 2;
	}

	public Double normalizedScore() {
		int totalQuantity = scores.stream()
				.mapToInt(Score::getQuantity)
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
