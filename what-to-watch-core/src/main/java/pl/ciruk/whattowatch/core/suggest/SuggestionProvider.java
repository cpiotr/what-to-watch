package pl.ciruk.whattowatch.core.suggest;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static pl.ciruk.whattowatch.utils.concurrent.CompletableFutures.combineUsing;

public class SuggestionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // Enough to keep every rate-limited site busy. Each title holds pages of up to 640 KB while it's scored, so a
    // page of 100 at once filled a 512 MB heap.
    private static final int TITLES_IN_FLIGHT = 10;
    // Waiting for a turn blocks, so it happens on a thread of its own rather than in the pool that scores
    private static final Executor WAITING = command -> Thread.ofVirtual().name("WaitingForTurn").start(command);

    private final TitleProvider titles;
    private final DescriptionProvider descriptions;
    private final List<ScoresProvider> scoresProviders;
    private final ExecutorService executorService;
    private final Cache<Title, ScoredTitle> cache;
    private final Map<Title, CompletableFuture<ScoredTitle>> titlesInProgress = new ConcurrentHashMap<>();
    // Shared by all requests, such as the startup crawl and a page, so together they stay within the limit
    private final Semaphore turns = new Semaphore(TITLES_IN_FLIGHT, true);

    public SuggestionProvider(
            TitleProvider titles,
            DescriptionProvider descriptions,
            List<ScoresProvider> scoresProviders,
            ExecutorService executorService,
            Cache<Title, ScoredTitle> cache) {
        this.titles = titles;
        this.descriptions = descriptions;
        this.scoresProviders = scoresProviders;
        this.executorService = executorService;
        this.cache = cache;
    }

    public Stream<CompletableFuture<ScoredTitle>> suggest(TitleKind kind, int pageNumber) {
        return suggest(kind, pageNumber, () -> false);
    }

    // Titles still waiting for a turn once the request is stopped, such as a page closed early, aren't searched
    public Stream<CompletableFuture<ScoredTitle>> suggest(TitleKind kind, int pageNumber, BooleanSupplier stopped) {
        LOGGER.info("Kind: {}, page number: {}", kind, pageNumber);

        return titles.streamOfTitles(kind, pageNumber)
                .map(title -> inTurn(title, stopped));
    }

    // A title that is already scored needs no turn
    private CompletableFuture<ScoredTitle> inTurn(Title title, BooleanSupplier stopped) {
        var cached = cache.getIfPresent(title);
        if (cached != null) {
            return completedFuture(cached);
        }
        return CompletableFuture.runAsync(this::awaitTurn, WAITING)
                .thenCompose(ignored -> {
                    if (stopped.getAsBoolean()) {
                        turns.release();
                        return completedFuture(ScoredTitle.empty());
                    }
                    return getOrScore(title).whenComplete((scored, error) -> turns.release());
                });
    }

    private void awaitTurn() {
        try {
            turns.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a turn to search", e);
        }
    }

    // Overlapping requests, such as the startup crawl and a user's, share one search for each title
    // A title knows its kind, so a series and a film with the same name and year are cached apart
    private CompletableFuture<ScoredTitle> getOrScore(Title title) {
        var cached = cache.getIfPresent(title);
        if (cached != null) {
            return completedFuture(cached);
        }
        var scored = titlesInProgress.computeIfAbsent(title, this::score);
        scored.whenComplete((ignored, error) -> titlesInProgress.remove(title, scored));
        return scored;
    }

    private CompletableFuture<ScoredTitle> score(Title title) {
        return CompletableFuture.supplyAsync(() -> descriptions.findDescriptionBy(title), executorService)
                .thenCompose(description -> description.map(this::scoresOf).orElseGet(() -> getEmpty(title)))
                .exceptionally(t -> {
                    LOGGER.error("Cannot score title {}", title, t);
                    return ScoredTitle.empty();
                });
    }

    private CompletableFuture<ScoredTitle> getEmpty(Title title) {
        var empty = ScoredTitle.empty();
        cache.put(title, empty);
        return completedFuture(empty);
    }

    private CompletableFuture<ScoredTitle> scoresOf(Description description) {
        var anyProviderFailed = new AtomicBoolean();
        var scoresFuture = scoresProviders.stream()
                .map(scoresProvider -> findScoresOrNone(scoresProvider, description, anyProviderFailed))
                .reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat, executorService));
        return scoresFuture
                .thenApply(stream -> stream.collect(toList()))
                .thenApply(scores -> new ScoredTitle(description, scores))
                .thenApply(scored -> {
                    // A title with missing scores isn't cached, so the next request retries the failed providers
                    if (!anyProviderFailed.get()) {
                        cache.put(scored.description().getFoundFor(), scored);
                    }
                    return scored;
                });
    }

    private CompletableFuture<Stream<Score>> findScoresOrNone(
            ScoresProvider scoresProvider,
            Description description,
            AtomicBoolean anyProviderFailed) {
        // Collected here, so errors thrown while a lazy stream runs are caught below too
        return CompletableFuture.supplyAsync(() -> scoresProvider.findScoresBy(description).toList(), executorService)
                .exceptionally(e -> {
                    LOGGER.warn("Cannot get scores from {} for {}", scoresProvider.getClass().getSimpleName(), description, e);
                    anyProviderFailed.set(true);
                    return List.of();
                })
                .thenApply(List::stream);
    }
}
