package pl.ciruk.whattowatch.score.imdb;

import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.google.GoogleScores;
import pl.ciruk.whattowatch.title.Title;

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

	public static void main(String[] args) {
		IMDBScores scores = new IMDBScores(new JsoupCachedConnection(CacheProvider.empty()));
		scores.scoresOf(
				Description.builder()
						.title(Title.builder().title("Citizenfour").originalTitle("Citizenfour").year(2014).build())
						.build())
				.forEach(System.out::println);
	}
}
