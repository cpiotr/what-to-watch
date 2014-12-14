package pl.ciruk.films.whattowatch.score;

import java.util.stream.Stream;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.title.Title;

public class IMDBScores implements ScoresProvider {

	ScoresProvider dataSource = new GoogleScores(new JsoupConnection(), "imdb"); 
	
	@Override
	public Stream<Score> scoresOf(Description description) {
		return dataSource.scoresOf(description);
	}

	public static void main(String[] args) {
		IMDBScores scores = new IMDBScores();
		scores.scoresOf(new Description(new Title("Rambo III", "Rambo III", 1988)))
				.forEach(System.out::println);
	}
}
