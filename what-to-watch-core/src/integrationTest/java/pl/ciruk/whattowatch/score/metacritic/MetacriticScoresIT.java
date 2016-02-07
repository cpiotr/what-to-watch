package pl.ciruk.whattowatch.score.metacritic;

import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.Assert.assertThat;
import static pl.ciruk.whattowatch.score.ScoreMatchers.isMeaningful;

public class MetacriticScoresIT {
	private JsoupCachedConnection connection;
	private MetacriticScores scores;

	@Before
	public void setUp() throws Exception {
		connection = new JsoupCachedConnection(CacheProvider.<String>empty(), new OkHttpClient());
		connection.init();
		scores = new MetacriticScores(connection, Executors.newSingleThreadExecutor());
	}

	@Test
	public void shouldRetrieveMeaningfulScore() throws Exception {
		Title title = titleOfRespectfulFilm();
		Description description = Description.builder()
				.title(title)
				.build();

		Stream<Score> scoreStream = scores.scoresOf(description);
		Score score = scoreStream
				.findAny()
				.orElseThrow(AssertionError::new);

		assertThat(score, isMeaningful());
	}

	private Title titleOfRespectfulFilm() {
		return Title.builder()
				.originalTitle("Harry Potter and the Deathly Hallows: Part 2")
				.year(2011)
				.build();
	}
}