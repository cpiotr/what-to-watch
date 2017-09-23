package pl.ciruk.whattowatch.boundary;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ManagedAsync;
import pl.ciruk.core.concurrent.AsyncExecutionException;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.Film;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;

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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Named
@Path("/suggestions")
@Slf4j
public class Suggestions {
    private final FilmSuggestionProvider suggestions;
    private final Timer responses;

    @Inject
    public Suggestions(FilmSuggestionProvider suggestions, MetricRegistry metricRegistry) {
        this.suggestions = suggestions;

        responses = metricRegistry.timer(name(Suggestions.class, "responses"));
    }

    @GET
    @Path("{pageNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @ManagedAsync
    public void get(
            @Suspended final AsyncResponse asyncResponse,
            @PathParam("pageNumber") int pageNumber) {
        log.info("get - Page number: {}", pageNumber);

        asyncResponse.setTimeout(90, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar ->
                ar.resume(
                        Response.status(SERVICE_UNAVAILABLE).entity("{\"error\":\"Request timed out\"}").build()
                )
        );

        Timer.Context time = responses.time();
        try {
            List<FilmResult> films = CompletableFutures.getAllOf(suggestions.suggestFilms(pageNumber))
                    .distinct()
                    .filter(Film::isWorthWatching)
                    .map(this::toFilmResult)
                    .collect(toList());

            asyncResponse.resume(Response.ok(films).build());
        } catch (AsyncExecutionException e) {
            asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
        } finally {
            time.stop();
        }
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

    @Builder
    @Getter
    static class FilmResult {
        String title;
        Integer year;
        String plot;
        String link;
        String poster;
        Double score;
        Integer numberOfScores;
        List<Score> scores;
        List<String> genres;
    }
}
