package pl.ciruk.whattowatch.boundary;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ManagedAsync;
import org.springframework.stereotype.Component;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Component
@Path("/scores")
@Slf4j
public class Scores {
    List<ScoresProvider> scoresProviders;

    @Inject
    public Scores(List<ScoresProvider> scoresProviders) {
        log.info("Scores create");
        this.scoresProviders = scoresProviders;
    }

    @PostConstruct
    void init() {
        log.info("init - ScoresProviders: {}", scoresProviders);
    }

    @GET
    @ManagedAsync
    @Produces("application/json")
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

		Function<ScoresProvider, CompletableFuture<Boolean>> toScoresOfAsync =
				scoresProvider -> scoresProvider.scoresOfAsync(description)
						.thenApply(scores -> scores.collect(toList()))
						.thenApply(asyncResponse::resume)
						.exceptionally(
								e -> asyncResponse.resume(
										Response.status(INTERNAL_SERVER_ERROR).entity(e).build()
								)
						);
		CompletableFutures.allOf(
				scoresProviders.stream()
						.map(toScoresOfAsync)
						.collect(Collectors.toList())
		);

        asyncResponse.setTimeout(1000, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(
				Response.status(SERVICE_UNAVAILABLE).entity("Request 11timed out").build()));
    }
}
