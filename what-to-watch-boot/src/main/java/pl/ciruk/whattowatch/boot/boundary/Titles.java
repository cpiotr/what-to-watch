package pl.ciruk.whattowatch.boot.boundary;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.core.title.TitleProvider;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Component
@Path("/titles")
public class Titles {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TitleProvider titleProvider;

    @Inject
    public Titles(TitleProvider titleProvider) {
        this.titleProvider = titleProvider;
    }

    @GET
    @Path("{pageNumber}")
    @ManagedAsync
    @Produces(MediaType.APPLICATION_JSON)
    public void findAll(
            @Suspended AsyncResponse asyncResponse,
            @PathParam("pageNumber") int pageNumber) {
        LOGGER.info("Page number: {}", pageNumber);

        asyncResponse.resume(
                Response.ok(titleProvider.streamOfTitles(pageNumber).collect(toList())).build()
        );

        asyncResponse.setTimeout(20, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));
    }
}
