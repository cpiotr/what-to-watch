package pl.ciruk.whattowatch.boot.boundary;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.filter.SuggestionFilter;
import pl.ciruk.whattowatch.core.suggest.ScoredTitle;
import pl.ciruk.whattowatch.core.suggest.SuggestionProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.neural.PersonalRatings;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.metrics.Names;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Named
@Path("/suggestions")
public class Suggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SuggestionProvider suggestionsProvider;
    private final SuggestionFilter suggestionFilter;
    private final PersonalRatings personalRatings;
    private final Timer responseTimer;

    @Inject
    public Suggestions(SuggestionProvider suggestionsProvider, SuggestionFilter suggestionFilter, PersonalRatings personalRatings) {
        this.suggestionsProvider = suggestionsProvider;
        this.suggestionFilter = suggestionFilter;
        this.personalRatings = personalRatings;

        responseTimer = Metrics.timer(Names.createName(Suggestions.class, "response"));
    }

    @GET
    @Path("{pageNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @ManagedAsync
    public void get(
            @Suspended AsyncResponse asyncResponse,
            @PathParam("pageNumber") int pageNumber,
            @QueryParam("kind") @DefaultValue("FILM") TitleKind kind) {
        LOGGER.info("Kind: {}, page number: {}", kind, pageNumber);

        asyncResponse.setTimeout(90, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Responses.requestTimedOut()));

        try {
            var suggestions = responseTimer.record(() -> findSuggestions(kind, pageNumber));

            asyncResponse.resume(Response.ok(suggestions).build());
            LOGGER.info("Finished getting suggestions for page {}", pageNumber);
        } catch (RuntimeException e) {
            LOGGER.error("Error while getting suggestions", e);
            asyncResponse.resume(Responses.internalError());
        }
    }

    @GET
    @Path("{pageNumber}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @ManagedAsync
    public void stream(
            @Context SseEventSink sseEventSink,
            @Context Sse sse,
            @PathParam("pageNumber") int pageNumber,
            @QueryParam("kind") @DefaultValue("FILM") TitleKind kind) {
        LOGGER.info("Stream kind: {}, page number: {}", kind, pageNumber);

        try (EventsSink sink = new EventsSink(sse, sseEventSink)) {
            responseTimer.record(() -> {
                // Every search starts before any is awaited, so the rate-limited sites work at the same time
                var sent = suggestionsProvider.suggest(kind, pageNumber)
                        .map(sendToSinkIfWorthWatching(sink))
                        .toList();
                sent.forEach(CompletableFuture::join);
            });
            LOGGER.info("Finished streaming suggestions for page {}", pageNumber);
        } catch (Exception e) {
            LOGGER.error("Error while getting suggestions", e);
        }
    }

    private Function<CompletableFuture<ScoredTitle>, CompletableFuture<Void>> sendToSinkIfWorthWatching(EventsSink sink) {
        return futureTitle -> futureTitle.thenAccept(
                scored -> {
                    if (suggestionFilter.isWorthWatching(scored)) {
                        SuggestionResult result = toSuggestionResult(scored);
                        sink.send(result);
                    }
                }
        );
    }

    private List<SuggestionResult> findSuggestions(TitleKind kind, int pageNumber) {
        return CompletableFutures.getAllOf(suggestionsProvider.suggest(kind, pageNumber))
                .distinct()
                .filter(suggestionFilter::isWorthWatching)
                .map(this::toSuggestionResult)
                .collect(toList());
    }

    private SuggestionResult toSuggestionResult(ScoredTitle scored) {
        var description = scored.description();
        return new SuggestionResult(
                description.titleAsText(),
                description.getYear(),
                description.getPlot(),
                description.getFoundFor().url(),
                description.getPoster(),
                scored.normalizedScore(),
                personalRatings.predict(scored),
                scored.scores(),
                description.getGenres());
    }

    static class EventsSink implements AutoCloseable {
        private final Sse sse;
        private final SseEventSink eventSink;

        EventsSink(Sse sse, SseEventSink eventSink) {
            this.sse = sse;
            this.eventSink = eventSink;
        }

        void send(SuggestionResult result) {
            OutboundSseEvent event = this.sse.newEventBuilder()
                    .name("title")
                    .id(String.valueOf(result.hashCode()))
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(SuggestionResult.class, result)
                    .build();
            this.eventSink.send(event);
        }

        @Override
        public void close() throws IOException {
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
