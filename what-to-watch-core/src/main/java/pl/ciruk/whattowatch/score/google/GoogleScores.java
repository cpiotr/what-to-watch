package pl.ciruk.whattowatch.score.google;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.text.NumberTokenizer;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;

import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class GoogleScores implements ScoresProvider {
	private final HttpConnection<Element> connection;

	private final ExecutorService executorService;

	private String sourcePage;

	public GoogleScores(@Named("noCookiesHtml") HttpConnection<Element> connection, ExecutorService executorService, String sourcePage) {
		this.connection = connection;
		this.executorService = executorService;
		this.sourcePage = sourcePage;
	}

	@Override
	public CompletableFuture<Stream<Score>> scoresOfAsync(Description description) {
		return CompletableFuture.supplyAsync(
				() -> scoresOf(description),
				executorService
		);
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
		return connection.connectToAndGet(url)
				.flatMap(GoogleSelectors.SCORE::extractFrom);
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
