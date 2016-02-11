package pl.ciruk.whattowatch.score.filmweb;

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

@Named("Filmweb")
@Slf4j
public class FilmwebScores implements ScoresProvider {
	private final ExecutorService executorService;

	ScoresProvider dataSource;

	@Inject
	public FilmwebScores(@Named("noCookies") JsoupConnection jsoupConnection, ExecutorService executorService) {
		this.executorService = executorService;
		dataSource = new GoogleScores(jsoupConnection, this.executorService, "filmweb");
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
