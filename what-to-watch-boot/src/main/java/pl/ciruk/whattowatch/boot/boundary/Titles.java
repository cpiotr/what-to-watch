package pl.ciruk.whattowatch.boot.boundary;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.TitleProvider;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

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
            @PathParam("pageNumber") int pageNumber,
            @QueryParam("kind") @DefaultValue("FILM") TitleKind kind) {
        LOGGER.info("Kind: {}, page number: {}", kind, pageNumber);

        asyncResponse.setTimeout(20, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));
        try {
            asyncResponse.resume(Response.ok(titleProvider.streamOfTitles(kind, pageNumber).toList()).build());
        } catch (RuntimeException e) {
            LOGGER.error("Error while getting titles", e);
            asyncResponse.resume(Responses.internalError());
        }
    }
}
