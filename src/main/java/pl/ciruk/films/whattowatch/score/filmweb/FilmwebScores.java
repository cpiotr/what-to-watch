package pl.ciruk.films.whattowatch.score.filmweb;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.score.google.GoogleScores;
import pl.ciruk.films.whattowatch.title.Title;

import java.util.stream.Stream;

public class FilmwebScores implements ScoresProvider {

	ScoresProvider dataSource = new GoogleScores(new JsoupConnection(), "filmweb");
	
	@Override
	public Stream<Score> scoresOf(Description description) {
		return dataSource.scoresOf(description);
	}

	public static void main(String[] args) {
		FilmwebScores scores = new FilmwebScores();
		scores.scoresOf(new Description(new Title("Rambo III", "Rambo III", 1988)))
				.forEach(System.out::println);
	}
}
