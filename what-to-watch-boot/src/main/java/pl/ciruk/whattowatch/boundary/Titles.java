package pl.ciruk.whattowatch.boundary;

import org.glassfish.jersey.server.ManagedAsync;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Component
@Path("/titles")
public class Titles {
	TitleProvider titles;

	@Inject
	public Titles(TitleProvider titles) {
		this.titles = titles;
	}

	@GET
	@ManagedAsync
	@Produces("application/json")
	public void findAll(@Suspended AsyncResponse asyncResponse) {
		asyncResponse.resume(
				Response.ok(titles.streamOfTitles().collect(toList())).build()
		);

		asyncResponse.setTimeout(20_000, TimeUnit.MILLISECONDS);
		asyncResponse.setTimeoutHandler(ar -> ar.resume(
				Response.status(SERVICE_UNAVAILABLE).entity("Request timed out").build()));
	}
}
