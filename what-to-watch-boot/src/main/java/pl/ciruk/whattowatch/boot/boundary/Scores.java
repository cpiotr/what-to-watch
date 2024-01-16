package pl.ciruk.whattowatch.boot.boundary;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static pl.ciruk.whattowatch.utils.concurrent.CompletableFutures.combineUsing;

@Component
@Path("/scores")
public class Scores {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<ScoresProvider> scoresProviders;

    @Inject
    public Scores(List<ScoresProvider> scoresProviders) {
        this.scoresProviders = scoresProviders;
    }

    @PostConstruct
    void init() {
        LOGGER.debug("ScoresProviders: {}", scoresProviders);
    }

    @GET
    @ManagedAsync
    @Produces(MediaType.APPLICATION_JSON)
    public void scoresFor(
            @Suspended final AsyncResponse asyncResponse,
            @QueryParam("title") String title,
            @QueryParam("originalTitle") String originalTitle,
            @QueryParam("year") int year) {
        Title titleObj = Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .build();

        Description description = Description.builder()
                .title(titleObj)
                .build();

        Function<ScoresProvider, CompletableFuture<Stream<Score>>> toScoresOfAsync =
                scoresProvider -> scoresProvider.findScoresByAsync(description);

        scoresProviders.stream()
                .map(toScoresOfAsync)
                .reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat))
                .thenApply(stream -> stream.collect(toList()))
                .thenApply(asyncResponse::resume)
                .exceptionally(e ->
                        asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));
    }
}
