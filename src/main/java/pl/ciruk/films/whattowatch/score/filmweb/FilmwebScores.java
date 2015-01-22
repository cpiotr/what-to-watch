package pl.ciruk.films.whattowatch.score.filmweb;

import pl.ciruk.films.whattowatch.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.score.google.GoogleScores;
import pl.ciruk.films.whattowatch.title.Title;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.stream.Stream;

@Named("Filmweb")
public class FilmwebScores implements ScoresProvider {

	ScoresProvider dataSource;

	@Inject
	public FilmwebScores(JsoupConnection jsoupConnection) {
		dataSource = new GoogleScores(jsoupConnection, "filmweb");
	}

	@Override
	public Stream<Score> scoresOf(Description description) {
		return dataSource.scoresOf(description);
	}

	public static void main(String[] args) {
		FilmwebScores scores = new FilmwebScores(null);
		scores.scoresOf(
				Description.builder()
						.title(Title.builder().title("Rambo III").originalTitle("Rambo III").year(1988).build())
						.build())
				.forEach(System.out::println);
	}
}
