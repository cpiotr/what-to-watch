package pl.ciruk.whattowatch.benchmark;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.jodah.failsafe.CircuitBreaker;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import pl.ciruk.whattowatch.boot.cache.RedisCache;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptionProvider;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScoresProvider;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.suggest.ScoredTitle;
import pl.ciruk.whattowatch.core.suggest.SuggestionProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.CachedConnection;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;
import redis.clients.jedis.RedisClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * What it costs to score titles whose pages are in Redis, as when a page of suggestions is shown again: each page is
 * read from Redis, decompressed and parsed, and the scores are found in it. The sites' rate limits make a title whose
 * pages aren't cached slower by seconds, so that isn't measured. IMDb's scores come from an index in memory, without
 * pages, so they are left out.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
@State(Scope.Benchmark)
public class CachedScoringBenchmark {
    static final List<Title> TITLES = List.of(
            Title.builder().title("Barbie").originalTitle("Barbie").year(2023).build(),
            Title.builder().title("Oppenheimer").originalTitle("Oppenheimer").year(2023).build());

    private GenericContainer<?> redis;
    private RedisClient redisClient;
    private ExecutorService executorService;
    private Cache<Title, ScoredTitle> scoredTitles;
    private SuggestionProvider suggestions;

    @Setup(Level.Trial)
    public void fillRedis() {
        redis = new GenericContainer<>(DockerImageName.parse("redis:6.2.24-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());
        redis.start();
        redisClient = RedisClient.create(redis.getHost(), redis.getFirstMappedPort());
        var cache = new RedisCache(redisClient, "benchmark", Duration.ofHours(1), new CircuitBreaker<>());
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        scoredTitles = Caffeine.newBuilder().build();
        suggestions = suggestionsFrom(new CachedConnection(cache, RecordedPages.load()), executorService, scoredTitles);

        // The first search reads the recorded pages and puts them in Redis
        requireCriticAndAudienceScores(scoreAll());
    }

    @Benchmark
    public List<ScoredTitle> scoreFromRedis() {
        scoredTitles.invalidateAll();
        return scoreAll();
    }

    @TearDown(Level.Trial)
    public void stopRedis() {
        executorService.close();
        redisClient.close();
        redis.stop();
    }

    // As the app scores them, without the rate limiter and the sites
    static SuggestionProvider suggestionsFrom(
            HttpConnection<String> pages,
            ExecutorService executorService,
            Cache<Title, ScoredTitle> scoredTitles) {
        var filmweb = new FilmwebProxy(pages);
        return new SuggestionProvider(
                (kind, pageNumber) -> TITLES.stream(),
                new FilmwebDescriptionProvider(filmweb, executorService),
                List.of(new FilmwebScoresProvider(filmweb), new MetacriticScoresProvider(new JsoupConnection(pages))),
                executorService,
                scoredTitles);
    }

    // Pages recorded before a site changed its markup give no scores, and scoring nothing would look fast
    static void requireCriticAndAudienceScores(List<ScoredTitle> scored) {
        for (var title : scored) {
            var types = title.scores().stream().map(Score::type).distinct().count();
            if (types < ScoreType.values().length) {
                throw new IllegalStateException(
                        "The recorded pages miss scores, record them again: " + title.description() + " " + title.scores());
            }
        }
    }

    private List<ScoredTitle> scoreAll() {
        return TITLES.stream()
                .map(suggestions::getOrScore)
                .toList()
                .stream()
                .map(CompletableFuture::join)
                .toList();
    }
}
