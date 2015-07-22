package pl.ciruk.whattowatch.boundary;

import org.glassfish.jersey.server.ManagedAsync;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.title.TitleProvider;
import pl.ciruk.whattowatch.title.zalukaj.ZalukajTitles;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Component
@Path("/titles")
public class Titles {
	TitleProvider titles;

	@Inject
	public Titles(ZalukajTitles titles) {
		this.titles = titles;
	}

	@GET
	@ManagedAsync
	@Produces("application/json")
	public void findAll(@Suspended AsyncResponse asyncResponse) {
		titles.streamOfTitles()
				.reduce(completedFuture(Stream.empty()), (cf1, cf2) -> cf1.thenCombine(cf2, Stream::concat))
				.thenApply(stream -> stream.collect(toList()))
				.thenApply(asyncResponse::resume)
				.exceptionally(e ->
						asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

		asyncResponse.setTimeout(20_000, TimeUnit.MILLISECONDS);
		asyncResponse.setTimeoutHandler(ar -> ar.resume(
				javax.ws.rs.core.Response.status(SERVICE_UNAVAILABLE).entity("Request timed out").build()));
	}
}
