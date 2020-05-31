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
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.metrics.Names;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ConnectionCallback;
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
import java.util.concurrent.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static pl.ciruk.whattowatch.utils.concurrent.Threads.manageBlocking;

@Named
@Path("/suggestions")
public class Suggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmSuggestionProvider suggestionsProvider;
    private final FilmFilter filmFilter;
    private final Timer responseTimer;
    private final ExecutorService threadPool = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            Threads.createForkJoinThreadFactory("Suggestions"),
            Threads.createUncaughtExceptionHandler(),
            true);

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

        var future = CompletableFuture.supplyAsync(
                () -> manageBlocking(() -> findSuggestions(pageNumber)),
                threadPool);
        asyncResponse.register((ConnectionCallback) disconnected -> {
            LOGGER.info("Disconnected");
            future.cancel(true);
        });
        try {
            var films = responseTimer.record(future::join);

            asyncResponse.resume(Response.ok(films).build());
            LOGGER.info("Finished getting suggestions for page {}", pageNumber);
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
        LOGGER.info("Stream page number: {}", pageNumber);

        try (EventsSink sink = new EventsSink(sse, sseEventSink)) {
            responseTimer.record(() -> suggestionsProvider.suggestFilms(pageNumber)
                    .map(sendToSinkIfWorthWatching(sink))
                    .forEach(CompletableFuture::join));
            LOGGER.info("Finished streaming suggestions for page {}", pageNumber);
        } catch (Exception e) {
            LOGGER.error("Error while getting suggestions", e);
        }
    }

    private Function<CompletableFuture<Film>, CompletableFuture<Void>> sendToSinkIfWorthWatching(EventsSink sink) {
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
                .title(film.description().titleAsText())
                .year(film.description().getYear())
                .plot(film.description().getPlot())
                .poster(film.description().getPoster())
                .score(film.normalizedScore())
                .numberOfScores(film.scores().size())
                .scores(film.scores())
                .genres(film.description().getGenres())
                .link(film.description().getFoundFor().url())
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
