package pl.ciruk.whattowatch.score.imdb;

import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.google.GoogleScores;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.stream.Stream;

@Named("IMDB")
@Slf4j
public class IMDBScores implements ScoresProvider {

	ScoresProvider dataSource;

	@Inject
	public IMDBScores(JsoupConnection jsoupConnection) {
		dataSource = new GoogleScores(jsoupConnection, "imdb");
	}

	@Override
	public Stream<Score> scoresOf(Description description) {
		log.info("scoresOf - Description: {}", description);

		return dataSource.scoresOf(description);
	}
}
