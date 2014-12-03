package pl.ciruk.films.whattowatch;

import java.util.ArrayList;
import java.util.List;

import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.Score;

public class Film {

	
	List<Score> scores = new ArrayList<>();
	private Description description;
	
	public Film(Description description) {
		this.description = description;
	}
	
	public void add(Score score) {
		scores.add(score);
	}

	@Override
	public String toString() {
		double avgScore = scores.stream().mapToDouble(Score::getScore).average().getAsDouble();
		int totalQuantity = scores.stream().mapToInt(Score::getQuantity).sum();
		return String.format("Title: %s. Scores: %d. AvgScore: %f; TotalQuantity: %d.", description.getTitle(), scores.size(), avgScore, totalQuantity);
	}
}
