package pl.ciruk.whattowatch.core.score;

import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class GenericScoreProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ExecutorService executorService;
    private final ScoresFinder scoresFinder;
    private final String providerName;
    private final AtomicLong missingScores = new AtomicLong();

    public GenericScoreProvider(
            ExecutorService executorService,
            ScoresFinder scoresFinder) {
        this.executorService = executorService;
        this.scoresFinder = scoresFinder;
        this.providerName = scoresFinder.getName();

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(providerName)),
                missingScores,
                AtomicLong::get
        );
    }

    @Override
    public Stream<Score> findScoresBy(Description description) {
        LOGGER.debug("{} description: {}", providerName, description);

        var scores = scoresFinder.searchFor(description)
                .filter(scoreResult -> scoresFinder.matchesDescription(scoreResult, description))
                .map(scoresFinder::extractScore)
                .flatMap(Optional::stream)
                .filter(Score::isSignificant);

        AtomicBoolean containsResult = new AtomicBoolean(false);
        try {
            return scores.peek(score -> {
                containsResult.set(true);
                LOGGER.debug("{} Score for {}: {}", providerName, description, score);
            });
        } finally {
            if (!containsResult.get()) {
                LOGGER.warn("{} Missing score for {}", providerName, description);
                missingScores.incrementAndGet();
            }
        }
    }

    @Override
    public CompletableFuture<Stream<Score>> findScoresByAsync(Description description) {
        return CompletableFuture.supplyAsync(
                () -> findScoresBy(description),
                executorService
        );
    }
}
