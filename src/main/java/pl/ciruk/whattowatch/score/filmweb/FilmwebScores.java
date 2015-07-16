package pl.ciruk.whattowatch.score.filmweb;

import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.google.GoogleScores;
import pl.ciruk.whattowatch.title.Title;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.stream.Stream;

@Named("Filmweb")
@Slf4j
public class FilmwebScores implements ScoresProvider {

	ScoresProvider dataSource;

	@Inject
	public FilmwebScores(JsoupConnection jsoupConnection) {
		dataSource = new GoogleScores(jsoupConnection, "filmweb");
	}

	@Override
	public Stream<Score> scoresOf(Description description) {
		log.info("scoresOf - Description: {}", description);

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
