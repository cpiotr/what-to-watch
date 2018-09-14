package pl.ciruk.whattowatch.boot.boundary;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.core.title.TitleProvider;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Component
@Path("/titles")
public class Titles {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TitleProvider titles;

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
        LOGGER.info("findAll - Page number: {}", pageNumber);

        asyncResponse.resume(
                Response.ok(titles.streamOfTitles(pageNumber).collect(toList())).build()
        );

        asyncResponse.setTimeout(20, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));
    }
}
