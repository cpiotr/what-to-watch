package pl.ciruk.whattowatch.boot.boundary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;

import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static pl.ciruk.whattowatch.utils.concurrent.CompletableFutures.combineUsing;

@RestController
@RequestMapping("/resources/scores")
public class Scores {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<ScoresProvider> scoresProviders;
    private final ExecutorService executorService;

    public Scores(List<ScoresProvider> scoresProviders, ExecutorService executorService) {
        this.scoresProviders = scoresProviders;
        this.executorService = executorService;
    }

    @PostConstruct
    void init() {
        LOGGER.debug("ScoresProviders: {}", scoresProviders);
    }

    @GetMapping
    public CompletableFuture<List<Score>> scoresFor(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String originalTitle,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "FILM") TitleKind kind) {
        Title titleObj = Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .kind(kind)
                .build();

        Description description = Description.builder()
                .title(titleObj)
                .build();

        Function<ScoresProvider, CompletableFuture<Stream<Score>>> toScoresOfAsync = scoresProvider -> CompletableFuture
                .supplyAsync(() -> scoresProvider.findScoresBy(description).toList(), executorService)
                // A site that fails, such as a throttled one, leaves the other sites' scores
                .exceptionally(e -> {
                    LOGGER.warn("Cannot get scores from {} for {}", scoresProvider.getClass().getSimpleName(), titleObj, e);
                    return List.of();
                })
                .thenApply(List::stream);

        return scoresProviders.stream()
                .map(toScoresOfAsync)
                .reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat))
                .thenApply(stream -> stream.collect(toList()))
                .orTimeout(10, SECONDS);
    }
}
