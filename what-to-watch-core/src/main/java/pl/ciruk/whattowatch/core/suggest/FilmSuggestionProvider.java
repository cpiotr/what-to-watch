package pl.ciruk.whattowatch.core.suggest;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static pl.ciruk.whattowatch.utils.concurrent.CompletableFutures.combineUsing;

public class FilmSuggestionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TitleProvider titles;
    private final DescriptionProvider descriptions;
    private final List<ScoresProvider> scoresProviders;
    private final ExecutorService executorService;
    private final Cache<Title, Film> cache;
    private final Map<Title, CompletableFuture<Film>> filmsInProgress = new ConcurrentHashMap<>();

    public FilmSuggestionProvider(
            TitleProvider titles,
            DescriptionProvider descriptions,
            List<ScoresProvider> scoresProviders,
            ExecutorService executorService,
            Cache<Title, Film> cache) {
        this.titles = titles;
        this.descriptions = descriptions;
        this.scoresProviders = scoresProviders;
        this.executorService = executorService;
        this.cache = cache;
    }

    public Stream<CompletableFuture<Film>> suggestFilms(int pageNumber) {
        LOGGER.info("Page number: {}", pageNumber);

        return titles.streamOfTitles(pageNumber)
                .map(this::getOrFindFilmByTitle);
    }

    // Overlapping requests, such as the startup crawl and a user's, share one search for each film
    private CompletableFuture<Film> getOrFindFilmByTitle(Title title) {
        var cachedFilm = cache.getIfPresent(title);
        if (cachedFilm != null) {
            return completedFuture(cachedFilm);
        }
        var film = filmsInProgress.computeIfAbsent(title, this::findFilmByTitle);
        film.whenComplete((ignored, error) -> filmsInProgress.remove(title, film));
        return film;
    }

    private CompletableFuture<Film> findFilmByTitle(Title title) {
        return CompletableFuture.supplyAsync(() -> descriptions.findDescriptionBy(title), executorService)
                .thenCompose(description -> description.map(this::descriptionToFilm).orElseGet(() -> getEmptyFilm(title)))
                .exceptionally(t -> {
                    LOGGER.error("Cannot get film for title {}", title, t);
                    return Film.empty();
                });
    }

    private CompletableFuture<Film> getEmptyFilm(Title title) {
        var emptyFilm = Film.empty();
        cache.put(title, emptyFilm);
        return completedFuture(emptyFilm);
    }

    private CompletableFuture<Film> descriptionToFilm(Description description) {
        var anyProviderFailed = new AtomicBoolean();
        var scoresFuture = scoresProviders.stream()
                .map(scoresProvider -> findScoresOrNone(scoresProvider, description, anyProviderFailed))
                .reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat, executorService));
        return scoresFuture
                .thenApply(stream -> stream.collect(toList()))
                .thenApply(scores -> new Film(description, scores))
                .thenApply(film -> {
                    // A film with missing scores isn't cached, so the next request retries the failed providers
                    if (!anyProviderFailed.get()) {
                        cache.put(film.description().getFoundFor(), film);
                    }
                    return film;
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
