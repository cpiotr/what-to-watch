package pl.ciruk.whattowatch.score.google;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.title.Title;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.ciruk.whattowatch.score.ScoreMatchers.isMeaningful;

public class GoogleScoresTest {
	private Document document;
	private JsoupConnection connection;

	@Before
	public void setUp() throws Exception {
		String searchResultsHTML = new String(
				Files.readAllBytes(
						Paths.get(
								getClass().getClassLoader().getResource("google-search-results.html").toURI())));
		connection = mock(JsoupConnection.class);
		document = Jsoup.parse(searchResultsHTML);
		when(connection.connectToAndGet(any())).thenReturn(Optional.of(document));
	}
	@Test
	public void shouldFindMeaningfulScoreForRambo() throws Exception {
		GoogleScores scores = new GoogleScores(connection, Executors.newSingleThreadExecutor(), "Test");

		Score score = scores.scoresOf(rambo())
				.findAny()
				.orElseThrow(AssertionError::new);

		assertThat(score, isMeaningful());
	}

	private Description rambo() {
		return Description.builder()
				.title(Title.builder().title("rambo").build())
				.build();
	}

}