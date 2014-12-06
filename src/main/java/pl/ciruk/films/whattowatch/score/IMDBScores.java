package pl.ciruk.films.whattowatch.score;

import java.util.stream.Stream;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.Description;

public class IMDBScores implements ScoresProvider {

	ScoresProvider dataSource = new GoogleScores(new JsoupConnection(), "imdb"); 
	
	@Override
	public Stream<Score> scoresOf(Description description) {
		return dataSource.scoresOf(description);
	}

}
