package pl.ciruk.whattowatch.core.suggest;

import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import pl.ciruk.whattowatch.boot.WhatToWatchApplication;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptionProvider;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScoresProvider;
import pl.ciruk.whattowatch.core.score.imdb.ImdbScoresProvider;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 5)
@Measurement(iterations = 10, time = 5)
@Fork(1)
@State(Scope.Thread)
@SuppressWarnings("PMD")
public class FilmSuggestionsBenchmark {
    private static final int NUMBER_OF_TITLES = 200;
    private static final int NUMBER_OF_THREADS = 16;

    private FilmSuggestionProvider suggestionsWorkStealing;

    private FilmSuggestionProvider suggestionsFixedPool;
    private ExecutorService workStealingPool;
    private ExecutorService fixedPool;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FilmSuggestionsBenchmark.class.getSimpleName())
                .shouldDoGC(false)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void initialize() {
        HttpConnection<String> connection = WhatToWatchApplication.createHttpConnection(WhatToWatchApplication.createHttpClient());

        workStealingPool = Executors.newWorkStealingPool(NUMBER_OF_THREADS);
        suggestionsWorkStealing = new FilmSuggestionProvider(
                provideTitlesFromResource(),
                sampleDescriptionProvider(connection, workStealingPool),
                sampleScoreProviders(connection, workStealingPool),
                workStealingPool,
                Caffeine.newBuilder().build()
        );

        fixedPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        suggestionsFixedPool = new FilmSuggestionProvider(
                provideTitlesFromResource(),
                sampleDescriptionProvider(connection, fixedPool),
                sampleScoreProviders(connection, fixedPool),
                fixedPool,
                Caffeine.newBuilder().build()
        );
    }

    @TearDown(Level.Trial)
    public void cleanUp() throws InterruptedException {
        fixedPool.shutdown();
        fixedPool.awaitTermination(1, TimeUnit.SECONDS);

        workStealingPool.shutdown();
        workStealingPool.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Benchmark
    public void workStealing(Blackhole bh) {
        Stream<Film> films = CompletableFutures.getAllOf(
                suggestionsWorkStealing.suggestFilms(1));
        int numberOfFilms = (int) films
                .filter(Objects::nonNull)
                .filter(Film::isNotEmpty)
                .count();

        bh.consume(numberOfFilms);
    }

    @Benchmark
    public void fixed(Blackhole bh) {
        Stream<Film> films = CompletableFutures.getAllOf(
                suggestionsFixedPool.suggestFilms(1));
        int numberOfFilms = (int) films
                .filter(Objects::nonNull)
                .filter(Film::isNotEmpty)
                .count();

        bh.consume(numberOfFilms);
    }

    private FilmwebDescriptionProvider sampleDescriptionProvider(HttpConnection<String> htmlConnection, ExecutorService pool) {
        return new FilmwebDescriptionProvider(
                new FilmwebProxy(new JsoupConnection(htmlConnection)),
                pool);
    }

    private static List<ScoresProvider> sampleScoreProviders(
            HttpConnection<String> connection,
            ExecutorService executorService) {
        JsoupConnection jsoupConnection = new JsoupConnection(connection);
        return List.of(
                new FilmwebScoresProvider(new FilmwebProxy(jsoupConnection), executorService),
                new MetacriticScoresProvider(jsoupConnection, executorService),
                new ImdbScoresProvider(jsoupConnection, executorService)
        );
    }

    private static TitleProvider provideTitlesFromResource() {
        String name = "films.csv";
        InputStream inputStream = getResourceAsStream(name);
        try (inputStream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<Title> titles = reader.lines()
                    .limit(NUMBER_OF_TITLES)
                    .map(line -> line.split(";"))
                    .map(array -> Title.builder().title(array[0]).originalTitle(array[1]).year(Integer.parseInt(array[2])).build())
                    .collect(toList());
            return (int pageNumber) -> titles.stream();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static InputStream getResourceAsStream(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(name);
        if (inputStream == null) {
            throw new IllegalStateException("Missing resource: " + name);
        }
        return inputStream;
    }

    private static CacheProvider<String> createJedisCache(final JedisPool pool) {
        return new CacheProvider<>() {
            @Override
            @SuppressFBWarnings(justification = "jedis")
            public void put(String key, String value) {
                try (Jedis jedis = pool.getResource()) {
                    jedis.set(key, value);
                }
            }

            @Override
            @SuppressFBWarnings(justification = "jedis")
            public Optional<String> get(String key) {
                try (Jedis jedis = pool.getResource()) {
                    return Optional.ofNullable(jedis.get(key));
                }
            }

            @Override
            public long removeAll(String keyExpression) {
                return 0;
            }
        };
    }

    private static JedisPool createJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(NUMBER_OF_THREADS);
        return new JedisPool(poolConfig, "172.17.0.2");
    }
}
