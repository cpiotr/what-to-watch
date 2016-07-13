package pl.ciruk.whattowatch.boundary;

import com.google.common.base.Stopwatch;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ManagedAsync;
import pl.ciruk.core.concurrent.AsyncExecutionException;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.Film;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Named
@Path("/suggestions")
@Slf4j
public class Suggestions {
	private final FilmSuggestionProvider suggestions;

	@Inject
	public Suggestions(FilmSuggestionProvider suggestions) {
		this.suggestions = suggestions;
	}

	@GET
	@Produces("application/json")
	@ManagedAsync
	public void get(@Suspended final AsyncResponse asyncResponse) {
		log.info("get");

		asyncResponse.setTimeout(60, TimeUnit.SECONDS);
		asyncResponse.setTimeoutHandler(ar -> ar.resume(
				Response.status(SERVICE_UNAVAILABLE).entity("Request timed out").build()));

		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			List<FilmResult> films = CompletableFutures.getAllOf(suggestions.suggestFilms())
					.filter(Film::isWorthWatching)
					.map(this::toFilmResult)
					.collect(toList());
			stopwatch.stop();

			asyncResponse.resume(Response.ok(films).build());
		} catch (AsyncExecutionException e) {
			asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
		} finally {
			log.debug("get - Request processed in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
		}
	}

	private FilmResult toFilmResult(Film film) {
		return FilmResult.builder()
				.title(film.getDescription().titleAsText())
				.year(film.getDescription().getYear())
				.plot(film.getDescription().getPlot())
				.poster(film.getDescription().getPoster())
				.score(film.normalizedScore())
				.numberOfScores(film.getScores().size())
				.genres(film.getDescription().getGenres())
				.link(film.getDescription().getFoundFor().getUrl())
				.build();
	}

	@Builder
	@Getter
	static class FilmResult {
		String title;
		Integer year;
		String plot;
		String link;
		String poster;
		Double score;
		Integer numberOfScores;
		List<String> genres;
	}
}
