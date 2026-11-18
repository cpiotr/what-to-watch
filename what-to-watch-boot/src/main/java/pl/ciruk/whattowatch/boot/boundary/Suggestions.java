package pl.ciruk.whattowatch.boot.boundary;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;
import pl.ciruk.whattowatch.core.filter.SuggestionFilter;
import pl.ciruk.whattowatch.core.suggest.ScoredTitle;
import pl.ciruk.whattowatch.core.suggest.SuggestionProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.neural.MyRatings;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.metrics.Names;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/resources/suggestions")
public class Suggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // A page of titles nobody searched for yet takes minutes at the sites' rate limits
    private static final long NO_TIMEOUT = 0L;

    private final SuggestionProvider suggestionsProvider;
    private final SuggestionFilter suggestionFilter;
    private final MyRatings myRatings;
    private final ExecutorService executorService;
    private final Timer responseTimer;

    public Suggestions(
            SuggestionProvider suggestionsProvider,
            SuggestionFilter suggestionFilter,
            MyRatings myRatings,
            ExecutorService executorService) {
        this.suggestionsProvider = suggestionsProvider;
        this.suggestionFilter = suggestionFilter;
        this.myRatings = myRatings;
        this.executorService = executorService;

        responseTimer = Metrics.timer(Names.createName(Suggestions.class, "response"));
    }

    @GetMapping("/{pageNumber}")
    public CompletableFuture<List<SuggestionResult>> get(
            @PathVariable int pageNumber,
            @RequestParam(defaultValue = "FILM") TitleKind kind) {
        LOGGER.info("Kind: {}, page number: {}", kind, pageNumber);

        return CompletableFuture.supplyAsync(() -> responseTimer.record(() -> findSuggestions(kind, pageNumber)), executorService)
                .orTimeout(90, SECONDS);
    }

    @GetMapping(path = "/{pageNumber}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable int pageNumber,
            @RequestParam(defaultValue = "FILM") TitleKind kind) {
        LOGGER.info("Stream kind: {}, page number: {}", kind, pageNumber);

        var emitter = new SseEmitter(NO_TIMEOUT);
        executorService.execute(() -> streamPage(emitter, kind, pageNumber));
        return emitter;
    }

    private void streamPage(SseEmitter emitter, TitleKind kind, int pageNumber) {
        try {
            responseTimer.record(() -> {
                // Every search starts before any is awaited, so the rate-limited sites work at the same time
                var sent = suggestionsProvider.suggest(kind, pageNumber)
                        .map(sendIfWorthWatching(emitter))
                        .toList();
                sent.forEach(CompletableFuture::join);
            });
            // Only a page searched to the end says so. A failed one just stops, and the page offers to retry it.
            send(emitter, SseEmitter.event()
                    .name("poisonPill")
                    .id(UUID.randomUUID().toString())
                    .data("poisonPill"));
            LOGGER.info("Finished streaming suggestions for page {}", pageNumber);
        } catch (RuntimeException e) {
            LOGGER.error("Error while getting suggestions", e);
        } finally {
            emitter.complete();
        }
    }

    private Function<CompletableFuture<ScoredTitle>, CompletableFuture<Void>> sendIfWorthWatching(SseEmitter emitter) {
        return futureTitle -> futureTitle.thenAccept(
                scored -> {
                    if (suggestionFilter.isWorthWatching(scored)) {
                        SuggestionResult result = toSuggestionResult(scored);
                        send(emitter, SseEmitter.event()
                                .name("title")
                                .id(String.valueOf(result.hashCode()))
                                .data(result, MediaType.APPLICATION_JSON));
                    }
                }
        );
    }

    // The page closes the stream when it no longer needs it, for example to show the other kind
    private static void send(SseEmitter emitter, SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            LOGGER.debug("Cannot send event, the stream is closed", e);
        }
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
        var title = description.getTitle();
        return new SuggestionResult(
                description.titleAsText(),
                title.localTitle(),
                title.originalTitle(),
                description.getYear(),
                title.kind(),
                description.getPlot(),
                description.getFoundFor().url(),
                description.getPoster(),
                scored.normalizedScore(),
                myRatings.predict(scored),
                scored.scores(),
                description.getGenres(),
                myRatings.ratingOf(title));
    }
}
