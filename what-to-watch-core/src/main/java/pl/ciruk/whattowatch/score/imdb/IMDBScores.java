package pl.ciruk.whattowatch.score.imdb;

import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.google.GoogleScores;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

@Named("IMDB")
@Slf4j
public class IMDBScores implements ScoresProvider {

	private final ScoresProvider dataSource;

	private final ExecutorService executorService;

	@Inject
	public IMDBScores(@Named("allCookies") JsoupConnection jsoupConnection, ExecutorService executorService) {
		this.executorService = executorService;
		dataSource = new GoogleScores(jsoupConnection, executorService, "imdb");
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

		return dataSource.scoresOf(description)
				.peek(score -> log.debug("scoresOf - Score for {}: {}", description, score));
	}
}
