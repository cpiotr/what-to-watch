package pl.ciruk.whattowatch.boot.boundary;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static pl.ciruk.whattowatch.utils.concurrent.CompletableFutures.combineUsing;

@Component
@Path("/scores")
public class Scores {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<ScoresProvider> scoresProviders;
    private final ExecutorService executorService;

    @Inject
    public Scores(List<ScoresProvider> scoresProviders, ExecutorService executorService) {
        this.scoresProviders = scoresProviders;
        this.executorService = executorService;
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
            @QueryParam("year") int year,
            @QueryParam("kind") @DefaultValue("FILM") TitleKind kind) {
        Title titleObj = Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .kind(kind)
                .build();

        Description description = Description.builder()
                .title(titleObj)
                .build();

        Function<ScoresProvider, CompletableFuture<Stream<Score>>> toScoresOfAsync = scoresProvider -> CompletableFuture
                .supplyAsync(() -> scoresProvider.findScoresBy(description).toList(), executorService)
                // A site that fails, such as a throttled one, leaves the other sites' scores
                .exceptionally(e -> {
                    LOGGER.warn("Cannot get scores from {} for {}", scoresProvider.getClass().getSimpleName(), titleObj, e);
                    return List.of();
                })
                .thenApply(List::stream);

        scoresProviders.stream()
                .map(toScoresOfAsync)
                .reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat))
                .thenApply(stream -> stream.collect(toList()))
                .thenApply(asyncResponse::resume)
                .exceptionally(e -> {
                    LOGGER.error("Error while getting scores for {}", titleObj, e);
                    return asyncResponse.resume(Responses.internalError());
                });

        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));
    }
}
