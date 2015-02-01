package pl.ciruk.films.whattowatch.score.imdb;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.score.google.GoogleScores;
import pl.ciruk.films.whattowatch.title.Title;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.stream.Stream;

@Named("IMDB")
public class IMDBScores implements ScoresProvider {

	ScoresProvider dataSource;

	@Inject
	public IMDBScores(JsoupConnection jsoupConnection) {
		dataSource = new GoogleScores(jsoupConnection, "imdb");
	}

	@Override
	public Stream<Score> scoresOf(Description description) {
		return dataSource.scoresOf(description);
	}

	public static void main(String[] args) {
		IMDBScores scores = new IMDBScores(null);
		scores.scoresOf(
				Description.builder()
						.title(Title.builder().title("Rambo III").originalTitle("Rambo III").year(1988).build())
						.build())
				.forEach(System.out::println);
	}
}
