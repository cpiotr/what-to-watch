package pl.ciruk.whattowatch.core.suggest;

import com.google.common.cache.Cache;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static pl.ciruk.whattowatch.utils.concurrent.CompletableFutures.combineUsing;

public class FilmSuggestions implements FilmSuggestionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TitleProvider titles;
    private DescriptionProvider descriptions;
    private List<ScoresProvider> scoresProviders;
    private final ExecutorService executorService;
    private final AtomicLong suggestedFilms = new AtomicLong();
    private final Cache<Title, Film> cache;


    public FilmSuggestions(
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

    @Override
    public Stream<CompletableFuture<Film>> suggestFilms(int pageNumber) {
        LOGGER.info("Page number: {}", pageNumber);

        return titles.streamOfTitles(pageNumber)
                .map(this::getOrFindFilmByTitle);
    }

    private CompletableFuture<Film> getOrFindFilmByTitle(Title title) {
        return Optional.ofNullable(cache.getIfPresent(title))
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> findFilmByTitle(title));

    }

    private CompletableFuture<Film> findFilmByTitle(Title title) {
        return descriptions.findDescriptionOfAsync(title)
                .thenComposeAsync(
                        description -> description.map(this::descriptionToFilm).orElse(completedFuture(Film.empty())),
                        executorService)
                .exceptionally(t -> {
                    LOGGER.error("Cannot get film for title {}", title, t);
                    return Film.empty();
                });
    }

    private CompletableFuture<Film> descriptionToFilm(Description description) {
        Function<ScoresProvider, CompletableFuture<Stream<Score>>> toScoresOfAsync =
                scoresProvider -> scoresProvider.findScoresByAsync(description);

        return scoresProviders.stream()
                .map(toScoresOfAsync)
                .reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat, executorService))
                .thenApply(stream -> stream.collect(toList()))
                .thenApply(scores -> Film.builder()
                        .description(description)
                        .scores(scores)
                        .build())
                .thenApply(recordFilm());
    }

    private UnaryOperator<Film> recordFilm() {
        return film -> {
            suggestedFilms.incrementAndGet();
            cache.put(film.getDescription().getFoundFor(), film);
            return film;
        };
    }
}
