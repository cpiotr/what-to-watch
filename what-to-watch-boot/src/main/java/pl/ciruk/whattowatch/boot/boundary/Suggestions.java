package pl.ciruk.whattowatch.boot.boundary;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.filter.FilmFilter;
import pl.ciruk.whattowatch.core.suggest.Film;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Named
@Path("/suggestions")
public class Suggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmSuggestionProvider suggestionsProvider;
    private final FilmFilter filmFilter;
    private final Timer responseTimer;

    @Inject
    public Suggestions(FilmSuggestionProvider suggestionsProvider, FilmFilter filmFilter) {
        this.suggestionsProvider = suggestionsProvider;
        this.filmFilter = filmFilter;

        responseTimer = Metrics.timer(Names.createName(Suggestions.class, "response"));
    }

    @GET
    @Path("{pageNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @ManagedAsync
    public void get(@Suspended AsyncResponse asyncResponse, @PathParam("pageNumber") int pageNumber) {
        LOGGER.info("Page number: {}", pageNumber);

        asyncResponse.setTimeout(90, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));

        try {
            var films = responseTimer.record(() -> findSuggestions(pageNumber));

            asyncResponse.resume(Response.ok(films).build());
            LOGGER.info("Finished providing suggestions for page {}", pageNumber);
        } catch (Exception e) {
            LOGGER.error("Error while getting suggestions", e);
            asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
        }
    }

    @GET
    @Path("{pageNumber}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @ManagedAsync
    public void stream(@Context SseEventSink sseEventSink, @Context Sse sse, @PathParam("pageNumber") int pageNumber) {
        LOGGER.info("Page number: {}", pageNumber);

        try (EventsSink sink = new EventsSink(sse, sseEventSink)) {
            responseTimer.record(() -> suggestionsProvider.suggestFilms(pageNumber)
                    .map(sendIfWorthWatchingTo(sink))
                    .forEach(CompletableFuture::join));
            LOGGER.info("Finished providing suggestions for page {}", pageNumber);
        } catch (Exception e) {
            LOGGER.error("Error while getting suggestions", e);
        }
    }

    private Function<CompletableFuture<Film>, CompletableFuture<Void>> sendIfWorthWatchingTo(EventsSink sink) {
        return futureFilm -> futureFilm.thenAccept(
                film -> {
                    if (filmFilter.isWorthWatching(film)) {
                        FilmResult filmResult = toFilmResult(film);
                        sink.send(filmResult);
                    }
                }
        );
    }

    private List<FilmResult> findSuggestions(int pageNumber) {
        return CompletableFutures.getAllOf(suggestionsProvider.suggestFilms(pageNumber))
                .distinct()
                .filter(filmFilter::isWorthWatching)
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

    static class EventsSink implements AutoCloseable {
        private final Sse sse;
        private final SseEventSink eventSink;

        EventsSink(Sse sse, SseEventSink eventSink) {
            this.sse = sse;
            this.eventSink = eventSink;
        }

        void send(FilmResult filmResult) {
            OutboundSseEvent event = this.sse.newEventBuilder()
                    .name("film")
                    .id(String.valueOf(filmResult.hashCode()))
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(FilmResult.class, filmResult)
                    .build();
            this.eventSink.send(event);
        }

        @Override
        public void close() {
            if (!eventSink.isClosed()) {
                OutboundSseEvent event = this.sse.newEventBuilder()
                        .name("poisonPill")
                        .id(UUID.randomUUID().toString())
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(String.class, "poisonPill")
                        .build();
                this.eventSink.send(event);
                eventSink.close();
            }
        }
    }
}
