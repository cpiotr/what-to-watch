package pl.ciruk.films.whattowatch.score.google;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Stream;

import pl.ciruk.films.whattowatch.net.JsoupConnection;
import pl.ciruk.core.text.NumberTokenizer;
import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.score.ScoresProvider;

public class GoogleScores implements ScoresProvider {
	private JsoupConnection connection;

	private String sourcePage;

	public GoogleScores(JsoupConnection connection, String sourcePage) {
		this.connection = connection;
		this.sourcePage = sourcePage;
	}
	
	@Override
	public Stream<Score> scoresOf(Description description) {
		String url = formatUrlBasedOn(description);
		return retrieveScoreFrom(url)
				.map(NumberTokenizer::new)
				.map(numberTokenizer -> {
					double rating = numberTokenizer.hasMoreTokens() ? numberTokenizer.nextToken().asNormalizedDouble() : -1;
					int quantity = numberTokenizer.hasMoreTokens() ? (int) numberTokenizer.nextToken().asNormalizedDouble() : -1;
					return new Score(rating, quantity);
				})
				.map(Stream::of)
				.orElse(Stream.empty());
	}

	private Optional<String> retrieveScoreFrom(String url) {
		return GoogleSelectors.SCORE.extractFrom(connection.connectToAndGet(url).get());
	}

	private String formatUrlBasedOn(Description description) {
		try {
			return String.format(
					"https://www.google.com/search?q=%s+%d+%s", 
					URLEncoder.encode(description.titleAsText(), Charset.defaultCharset().name()),
					description.getYear(),
					URLEncoder.encode(sourcePage, Charset.defaultCharset().name()));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
