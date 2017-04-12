package pl.ciruk.whattowatch.score.imdb;

import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.json.JsonConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.Assert.assertThat;
import static pl.ciruk.whattowatch.score.ScoreMatchers.isMeaningful;

public class ImdbScoresIT {
	private JsonConnection connection;
	private ImdbScores scores;

	@Before
	public void setUp() throws Exception {
		connection = new JsonConnection(new HtmlConnection(new OkHttpClient()));

		scores = new ImdbScores(connection, Executors.newSingleThreadExecutor());
	}

	@Test
	public void shouldRetrieveMeaningfulScoreOfOldFilm() throws Exception {
		Title title = titleOfOldAndRespectfulFilm();
		Description description = Description.builder()
				.title(title)
				.build();

		Stream<Score> scoreStream = scores.scoresOf(description);
		Score score = scoreStream
				.findAny()
				.orElseThrow(AssertionError::new);

		assertThat(score, isMeaningful());
	}

	private Title titleOfOldAndRespectfulFilm() {
		return Title.builder()
				.title("Rambo")
				.originalTitle("First blood")
				.year(1982)
				.build();
	}

	@Test
	public void shouldRetrieveMeaningfulScoreOfRecentFilm() throws Exception {
		Title title = titleOfRecentAndRespectfulFilm();
		Description description = Description.builder()
				.title(title)
				.build();

		Stream<Score> scoreStream = scores.scoresOf(description);
		Score score = scoreStream
				.findAny()
				.orElseThrow(AssertionError::new);

		assertThat(score, isMeaningful());
	}

	private Title titleOfRecentAndRespectfulFilm() {
		return Title.builder()
				.title("Vaiana")
				.originalTitle("Moana")
				.year(2016)
				.build();
	}
}