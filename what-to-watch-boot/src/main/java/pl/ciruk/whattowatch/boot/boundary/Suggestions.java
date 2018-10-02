package pl.ciruk.whattowatch.boot.boundary;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.suggest.Film;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.utils.concurrent.AsyncExecutionException;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.metrics.Names;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Named
@Path("/suggestions")
public class Suggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmSuggestionProvider suggestions;
    private final Timer responseTimer;

    @Inject
    public Suggestions(FilmSuggestionProvider suggestions) {
        this.suggestions = suggestions;

        responseTimer = Metrics.timer(Names.createName(Suggestions.class, "response"));
    }

    @GET
    @Path("{pageNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @ManagedAsync
    public void get(@Suspended AsyncResponse asyncResponse, @PathParam("pageNumber") int pageNumber) {
        LOGGER.info("get - Page number: {}", pageNumber);

        asyncResponse.setTimeout(90, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));

        try {
            var films = responseTimer.record(() -> findSuggestions(pageNumber));

            asyncResponse.resume(Response.ok(films).build());
        } catch (AsyncExecutionException e) {
            asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
        }
    }

    private List<FilmResult> findSuggestions(int pageNumber) {
        return CompletableFutures.getAllOf(suggestions.suggestFilms(pageNumber))
                .distinct()
                .filter(Film::isWorthWatching)
                .map(this::toFilmResult)
                .collect(toList());
    }

    private FilmResult toFilmResult(Film film) {
        return FilmResult.builder()
                .title(film.getDescription().titleAsText())
                .year(film.getDescription().getYear())
                .plot(film.getDescription().getPlot())
                .poster(film.getDescription().getPoster())
                .score(film.normalizedScore())
                .numberOfScores(film.getScores().size())
                .scores(film.getScores())
                .genres(film.getDescription().getGenres())
                .link(film.getDescription().getFoundFor().getUrl())
                .build();
    }
}
