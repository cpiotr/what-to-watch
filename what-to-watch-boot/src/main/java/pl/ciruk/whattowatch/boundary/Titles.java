package pl.ciruk.whattowatch.boundary;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ManagedAsync;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Component
@Path("/titles")
@Slf4j
public class Titles {
    TitleProvider titles;

    @Inject
    public Titles(TitleProvider titles) {
        this.titles = titles;
    }

    @GET
    @Path("{pageNumber}")
    @ManagedAsync
    @Produces(MediaType.APPLICATION_JSON)
    public void findAll(
            @Suspended AsyncResponse asyncResponse,
            @PathParam("pageNumber") int pageNumber) {
        log.info("findAll - Page number: {}", pageNumber);

        asyncResponse.resume(
                Response.ok(titles.streamOfTitles(pageNumber).collect(toList())).build()
        );

        asyncResponse.setTimeout(20_000, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(
                Response.status(SERVICE_UNAVAILABLE).entity("Request timed out").build()));
    }
}
