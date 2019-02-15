package pl.ciruk.whattowatch.core.suggest;

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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
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
    private final AtomicLong suggestedFilms = new AtomicLong();
    private final Map<Title, Film> cache;

    public FilmSuggestionProvider(
            TitleProvider titles,
            DescriptionProvider descriptions,
            List<ScoresProvider> scoresProviders,
            ExecutorService executorService,
            Map<Title, Film> cache) {
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

    private CompletableFuture<Film> getOrFindFilmByTitle(Title title) {
        return Optional.ofNullable(cache.get(title))
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

        var scoresFuture = scoresProviders.stream()
                .map(toScoresOfAsync)
                .reduce(completedFuture(Stream.empty()), combineByConcatenation());
        return scoresFuture
                .thenApply(stream -> stream.collect(toList()))
                .thenApply(scores -> createFilm(description, scores))
                .thenApply(recordFilm());
    }

    private Film createFilm(Description description, List<Score> scores) {
        return Film.builder()
                .description(description)
                .scores(scores)
                .build();
    }

    private BinaryOperator<CompletableFuture<Stream<Score>>> combineByConcatenation() {
        return combineUsing(Stream::concat, executorService);
    }

    private UnaryOperator<Film> recordFilm() {
        return film -> {
            suggestedFilms.incrementAndGet();
            cache.put(film.getDescription().getFoundFor(), film);
            return film;
        };
    }
}
