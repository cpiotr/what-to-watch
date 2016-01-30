package pl.ciruk.whattowatch.boundary;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ManagedAsync;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

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

		suggestions.suggestFilms()
				.thenApply(asyncResponse::resume)
				.exceptionally(e -> {
					log.error("get - Could not get suggestions", e);
					return asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
				});

		asyncResponse.setTimeout(60_000, TimeUnit.MILLISECONDS);
		asyncResponse.setTimeoutHandler(ar -> ar.resume(
				Response.status(SERVICE_UNAVAILABLE).entity("Request timed out").build()));

	}
}
