package pl.ciruk.whattowatch.core.score.imdb;

import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Scores from IMDb's non-commercial datasets, which IMDb allows for personal, non-commercial use
 * with the credit "Information courtesy of IMDb (https://www.imdb.com). Used with permission."
 */
public final class ImdbDatasetScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_IMDB_SCORE = 10;
    private static final String IMDB = "IMDb";
    private final ExecutorService executorService;
    private final AtomicLong missingScores = new AtomicLong();
    private final ImdbTitleIndex index;

    /**
     * Indexes {@code title.ratings.tsv.gz} and {@code title.basics.tsv.gz} from {@code datasetsDirectory}.
     */
    public ImdbDatasetScoresProvider(Path datasetsDirectory, ExecutorService executorService) {
        this.executorService = executorService;
        this.index = loadIndex(datasetsDirectory);

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(IMDB)),
                missingScores,
                AtomicLong::get
        );
    }

    private static ImdbTitleIndex loadIndex(Path datasetsDirectory) {
        try {
            var start = System.nanoTime();
            var index = ImdbTitleIndex.load(
                    datasetsDirectory.resolve("title.ratings.tsv.gz"),
                    datasetsDirectory.resolve("title.basics.tsv.gz"),
                    ImdbScoresProvider.NUMBER_OF_VOTES_LOWER_BOUND);
            LOGGER.info("Indexed {} IMDb titles in {} ms", index.size(), (System.nanoTime() - start) / 1_000_000);
            return index;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CompletableFuture<Stream<Score>> findScoresByAsync(Description description) {
        return CompletableFuture.supplyAsync(
                () -> findScoresBy(description),
                executorService
        );
    }

    @Override
    public Stream<Score> findScoresBy(Description description) {
        var score = index.find(description.getTitle())
                .map(this::toScore)
                .filter(found -> found.grade() > 0.0);
        if (score.isEmpty()) {
            LOGGER.warn("Missing score for {}", description);
            missingScores.incrementAndGet();
        }
        return score.stream()
                .peek(found -> LOGGER.debug("Score for {}: {}", description, found));
    }

    private Score toScore(ImdbTitleIndex.ImdbTitle title) {
        return Score.builder()
                .grade(title.rating() / MAX_IMDB_SCORE)
                .quantity(title.votes())
                .source(IMDB)
                .type(ScoreType.AMATEUR)
                .url("https://www.imdb.com/title/" + title.id())
                .build();
    }
}
