package pl.ciruk.whattowatch.score.filmweb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.google.GoogleScores;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.stream.Stream;

@Named("Filmweb")
@Slf4j
public class FilmwebScores implements ScoresProvider {

	ScoresProvider dataSource;

	@Inject
	public FilmwebScores(@Qualifier("allCookies")JsoupConnection jsoupConnection) {
		dataSource = new GoogleScores(jsoupConnection, "filmweb");
	}

	@Override
	public Stream<Score> scoresOf(Description description) {
		log.info("scoresOf - Description: {}", description);

		return dataSource.scoresOf(description);
	}
}
