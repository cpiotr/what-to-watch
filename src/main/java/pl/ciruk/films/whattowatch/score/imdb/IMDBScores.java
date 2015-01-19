package pl.ciruk.films.whattowatch.score.imdb;

import java.util.stream.Stream;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.google.GoogleScores;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.title.Title;

public class IMDBScores implements ScoresProvider {

	ScoresProvider dataSource = new GoogleScores(new JsoupConnection(), "imdb");
	
	@Override
	public Stream<Score> scoresOf(Description description) {
		return dataSource.scoresOf(description);
	}

	public static void main(String[] args) {
		IMDBScores scores = new IMDBScores();
		scores.scoresOf(
				Description.builder()
						.title(Title.builder().title("Rambo III").originalTitle("Rambo III").year(1988).build())
						.build())
				.forEach(System.out::println);
	}
}