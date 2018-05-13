package pl.ciruk.whattowatch.boot.boundary;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.core.concurrent.AsyncExecutionException;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.core.suggest.Film;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;

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
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Named
@Path("/suggestions")
public class Suggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmSuggestionProvider suggestions;
    private final Timer responseTimer;

    @Inject
    public Suggestions(FilmSuggestionProvider suggestions) {
        this.suggestions = suggestions;

        responseTimer = Metrics.timer(Suggestions.class.getSimpleName() + ".responses");
    }

    @GET
    @Path("{pageNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @ManagedAsync
    public void get(
            @Suspended final AsyncResponse asyncResponse,
            @PathParam("pageNumber") int pageNumber) {
        LOGGER.info("get - Page number: {}", pageNumber);

        asyncResponse.setTimeout(90, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar ->
                ar.resume(
                        Response.status(SERVICE_UNAVAILABLE).entity("{\"error\":\"Request timed out\"}").build()
                )
        );

        try {
            var films = responseTimer.record(() -> findSuggestions(pageNumber));

            asyncResponse.resume(Response.ok(films).build());
        } catch (AsyncExecutionException e) {
            asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
        }
    }

    private List<FilmResult> findSuggestions(@PathParam("pageNumber") int pageNumber) {
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

        FilmResult(String title, Integer year, String plot, String link, String poster, Double score, Integer numberOfScores, List<Score> scores, List<String> genres) {
            this.title = title;
            this.year = year;
            this.plot = plot;
            this.link = link;
            this.poster = poster;
            this.score = score;
            this.numberOfScores = numberOfScores;
            this.scores = scores;
            this.genres = genres;
        }

        static FilmResultBuilder builder() {
            return new FilmResultBuilder();
        }

        public String getTitle() {
            return title;
        }

        public Integer getYear() {
            return year;
        }

        public String getPlot() {
            return plot;
        }

        public String getLink() {
            return link;
        }

        public String getPoster() {
            return poster;
        }

        public Double getScore() {
            return score;
        }

        public Integer getNumberOfScores() {
            return numberOfScores;
        }

        public List<Score> getScores() {
            return scores;
        }

        public List<String> getGenres() {
            return genres;
        }
    }

    public static class FilmResultBuilder {
        private String title;
        private Integer year;
        private String plot;
        private String link;
        private String poster;
        private Double score;
        private Integer numberOfScores;
        private List<Score> scores;
        private List<String> genres;

        public FilmResultBuilder title(String title) {
            this.title = title;
            return this;
        }

        public FilmResultBuilder year(Integer year) {
            this.year = year;
            return this;
        }

        public FilmResultBuilder plot(String plot) {
            this.plot = plot;
            return this;
        }

        public FilmResultBuilder link(String link) {
            this.link = link;
            return this;
        }

        public FilmResultBuilder poster(String poster) {
            this.poster = poster;
            return this;
        }

        public FilmResultBuilder score(Double score) {
            this.score = score;
            return this;
        }

        public FilmResultBuilder numberOfScores(Integer numberOfScores) {
            this.numberOfScores = numberOfScores;
            return this;
        }

        public FilmResultBuilder scores(List<Score> scores) {
            this.scores = scores;
            return this;
        }

        public FilmResultBuilder genres(List<String> genres) {
            this.genres = genres;
            return this;
        }

        public FilmResult build() {
            return new FilmResult(title, year, plot, link, poster, score, numberOfScores, scores, genres);
        }
    }
}
