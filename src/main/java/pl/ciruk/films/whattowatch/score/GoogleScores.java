package pl.ciruk.films.whattowatch.score;

import static pl.ciruk.core.net.JsoupConnection.connectTo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import pl.ciruk.core.text.NumberTokenizer;
import pl.ciruk.films.whattowatch.description.Description;

public class GoogleScores implements ScoresProvider {

	private String sourcePage;

	public GoogleScores(String sourcePage) {
		this.sourcePage = sourcePage;
	}
	
	@Override
	public Stream<Score> scoresOf(Description description) {
		String url = formatUrlBasedOn(description);
		String scoreAsText = retrieveScoreFrom(url);
		
		NumberTokenizer numberTokenizer = new NumberTokenizer(scoreAsText);
		double score = numberTokenizer.nextToken().asNormalizedDouble();
		int quantity = (int) numberTokenizer.nextToken().asNormalizedDouble();
		return Stream.of(new Score(score, quantity));
	}

	private String retrieveScoreFrom(String url) {
		try {
			return connectTo(url).get()
					.select("ol#rso li.g div.slp")
					.stream()
					.map(e -> e.text())
					.filter(s -> !s.isEmpty())
					.findFirst()
					.get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String formatUrlBasedOn(Description description) {
		try {
			return String.format(
					"https://www.google.com/search?q=%s+%d+%s", 
					URLEncoder.encode(description.getTitle(), Charset.defaultCharset().name()), 
					description.getYear(),
					URLEncoder.encode(sourcePage, Charset.defaultCharset().name()));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
