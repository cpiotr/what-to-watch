package pl.ciruk.whattowatch.score.metacritic;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.text.MissingValueException;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.score.metacritic.MetacriticSelectors.LINK_TO_DETAILS;

@Named("Metacritic")
@Slf4j
public class MetacriticScores implements ScoresProvider {
	private static final String METACRITIC_BASE_URL = "http://www.metacritic.com";

	private static final int NYT_SCORE_WEIGHT = 1_000;

	private final HttpConnection connection;

	private final ExecutorService executorService;

	@Inject
	public MetacriticScores(@Named("noCookiesHtml") HttpConnection connection, ExecutorService executorService) {
		this.connection = connection;
		this.executorService = executorService;
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
		log.info("scoresOf - Description: {}", description);

		Optional<Element> htmlWithScores = metacriticSummaryOf(description.titleAsText(), description.getYear())
				.flatMap(LINK_TO_DETAILS::extractFrom)
				.flatMap(href -> downloadPage(METACRITIC_BASE_URL + href))
				.flatMap(MetacriticSelectors.LINK_TO_CRITIC_REVIEWS::extractFrom)
				.flatMap(href -> downloadPage(METACRITIC_BASE_URL + href))
				.map(page -> page.select("#main").first());

		Stream<Score> averageScoreStream = htmlWithScores
				.flatMap(htmlContent -> averageGradeFrom(htmlContent)
						.map(grade -> new Score(grade, numberOfReviewsFrom(htmlContent))))
				.map(Stream::of).orElseGet(Stream::empty);

		Stream<Score> nytScoreStream = htmlWithScores
				.flatMap(htmlContent -> nytScoreFrom(htmlContent))
				.map(Stream::of).orElseGet(Stream::empty);

		return Stream.concat(averageScoreStream, nytScoreStream)
				.peek(score -> log.debug("scoresOf - Score for {}: {}", description, score));
	}

	Optional<Double> averageGradeFrom(Element htmlWithScores) {
		return MetacriticSelectors.AVERAGE_GRADE.extractFrom(htmlWithScores)
				.map(Double::valueOf)
				.map(d -> d / 100.0);
	}
	
	int numberOfReviewsFrom(Element htmlWithScores) {
		return MetacriticSelectors.NUMBER_OF_GRADES.extractFrom(htmlWithScores)
				.map(Integer::valueOf)
				.orElseThrow(() -> new MissingValueException(htmlWithScores.text()));
	}
	
	Optional<Score> nytScoreFrom(Element htmlWithScores) {
		return MetacriticSelectors.NEW_YORK_TIMES_GRADE.extractFrom(htmlWithScores)
				.map(grade -> (Double.valueOf(grade) / 100.0))
				.map(percentage -> new Score(percentage, NYT_SCORE_WEIGHT));
	}
	
	private Optional<Element> downloadPage(String url) {
		return connection.connectToAndGet(url);
	}

	Optional<Element> metacriticSummaryOf(String title, int year) {
		try {
			Predicate<String> matchesTitle = matches(title);

			String searchUrl = String.format(
					METACRITIC_BASE_URL + "/search/movie/%s/results",
					URLEncoder.encode(title, Charsets.UTF_8.toString()));

			return downloadPage(searchUrl)
					.flatMap(page -> MetacriticStreamSelectors.SEARCH_RESULTS.extractFrom(page)
							.filter(e -> MetacriticSelectors.TITLE.extractFrom(e)
											.filter(matchesTitle)
											.isPresent()
							)
							.filter(e -> MetacriticSelectors.RELEASE_DATE.extractFrom(e)
											.filter(date -> date.endsWith(String.valueOf(year)))
											.isPresent()
							)
							.findFirst());
		} catch (Exception e) {
			log.warn("Cannot find metacritic summary of {}", title, e);
			return Optional.empty();
		}
	}

	private Predicate<String> matches(String title) {
		String titleOnlyAlphaNum = replaceNonAlphaNumWithSpace(title);
		return t -> replaceNonAlphaNumWithSpace(t).equalsIgnoreCase(titleOnlyAlphaNum);
	}

	private static String replaceNonAlphaNumWithSpace(String text) {
		return text.replaceAll("[^\\p{L}0-9 ]", " ");
	}
}
