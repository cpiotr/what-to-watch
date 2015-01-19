package pl.ciruk.films.whattowatch;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.title.Title;

import static pl.ciruk.core.math.WilsonScore.confidenceIntervalLowerBound;

public class Film {

	@Getter
	@Setter
	private String link;
	
	List<Score> scores = new ArrayList<>();

	private Description description;
	
	public Film(Description description) {
		this.description = description;
	}

	public void add(Score score) {
		scores.add(score);
	}

	public Title foundFor() {
		return description.getFoundFor();
	}

	@Override
	public String toString() {
		double avgScore = scores.stream().mapToDouble(Score::getScore).average().orElse(0.0);
		int totalQuantity = scores.stream().mapToInt(Score::getQuantity).sum();
		return String.format("Title: %s. Year: %d. Scores: %d. AvgScore: %f; TotalQuantity: %d.",
				description.titleAsText(),
				description.getYear(),
				scores.size(),
				avgScore,
				totalQuantity);
	}

	public String poster() {
		return description.getPoster();
	}

	public String title() {
		return description.titleAsText();
	}

	public Double score() {
		return scores.stream()
				.mapToDouble(Score::getScore)
				.average()
				.orElse(0.0);
	}

	public Double normalizedScore() {
		int totalQuantity = scores.stream()
				.mapToInt(Score::getQuantity)
				.sum();
		int positiveQuantity = (int) (score() * totalQuantity);
		return confidenceIntervalLowerBound(positiveQuantity, totalQuantity, 0.95);
	}

	public int numberOfScores() {
		return scores.size();
	}

	public String plot() {
		return description.getPlot();
	}

	public Integer year() {
		return description.getYear();
	}
}