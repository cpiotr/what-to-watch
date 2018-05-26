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

    public FilmSuggestions(
            TitleProvider titles,
            DescriptionProvider descriptions,
            List<ScoresProvider> scoresProviders,
            ExecutorService executorService) {
        this.titles = titles;
        this.descriptions = descriptions;
        this.scoresProviders = scoresProviders;
        this.executorService = executorService;
    }

    @Override
    public Stream<CompletableFuture<Film>> suggestFilms(int pageNumber) {
        LOGGER.info("suggestFilms");

        return titles.streamOfTitles(pageNumber)
                .map(this::findFilmForTitle);
    }

    private CompletableFuture<Film> findFilmForTitle(Title title) {
        return descriptions.descriptionOfAsync(title)
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
                scoresProvider -> scoresProvider.scoresOfAsync(description);

        return scoresProviders.stream()
                .map(toScoresOfAsync)
                .reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat, executorService))
                .thenApply(stream -> stream.collect(toList()))
                .thenApply(scores -> Film.builder()
                        .description(description)
                        .scores(scores)
                        .build())
                .thenApply(incrementCounter());
    }

    private UnaryOperator<Film> incrementCounter() {
        return film -> {
            suggestedFilms.incrementAndGet();
            return film;
        };
    }
}
