package pl.ciruk.films.whattowatch;

import java.util.ArrayList;
import java.util.List;

import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.title.Title;

public class Film {

	
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
}
