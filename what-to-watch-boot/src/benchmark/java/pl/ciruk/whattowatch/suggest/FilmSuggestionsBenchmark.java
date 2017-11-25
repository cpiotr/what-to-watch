package pl.ciruk.whattowatch.suggest;

import com.codahale.metrics.MetricRegistry;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.Film;
import pl.ciruk.whattowatch.WhatToWatchApplication;
import pl.ciruk.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.score.imdb.ImdbWebScores;
import pl.ciruk.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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
public class FilmSuggestionsBenchmark {
    private static final int NUMBER_OF_TITLES = 200;
    private static final int NUMBER_OF_THREADS = 16;

    private FilmSuggestions suggestionsWorkStealing;

    private FilmSuggestions suggestionsFixedPool;
    private ExecutorService workStealingPool;
    private ExecutorService fixedPool;

    @Setup(Level.Trial)
    public void initialize() {
        HttpConnection<String> connection = WhatToWatchApplication.createHttpConnection();
        MetricRegistry metricRegistry = new MetricRegistry();

        workStealingPool = Executors.newWorkStealingPool(NUMBER_OF_THREADS);
        suggestionsWorkStealing = new FilmSuggestions(
                provideTitlesFromResource(),
                sampleDescriptionProvider(connection, workStealingPool),
                sampleScoreProviders(connection, metricRegistry, workStealingPool),
                workStealingPool
        );

        fixedPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        suggestionsFixedPool= new FilmSuggestions(
                provideTitlesFromResource(),
                sampleDescriptionProvider(connection, fixedPool),
                sampleScoreProviders(connection, metricRegistry, fixedPool),
                fixedPool
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
    public void fixedPool(Blackhole bh) {
        Stream<Film> films = CompletableFutures.getAllOf(
                suggestionsFixedPool.suggestFilms(1));
        int numberOfFilms = (int) films
                .filter(Objects::nonNull)
                .filter(Film::isNotEmpty)
                .count();

        bh.consume(numberOfFilms);
    }

    private FilmwebDescriptions sampleDescriptionProvider(HttpConnection<String> htmlConnection, ExecutorService pool) {
        return new FilmwebDescriptions(
                new FilmwebProxy(new JsoupConnection(htmlConnection)),
                new MetricRegistry(),
                pool);
    }

    private static List<ScoresProvider> sampleScoreProviders(
            HttpConnection<String> connection,
            MetricRegistry metricRegistry,
            ExecutorService executorService) {
        JsoupConnection jsoupConnection = new JsoupConnection(connection);
        return List.of(
                new FilmwebScores(new FilmwebProxy(jsoupConnection), metricRegistry, executorService),
                new MetacriticScores(jsoupConnection, metricRegistry, executorService),
                new ImdbWebScores(jsoupConnection, metricRegistry, executorService)
        );
    }

    private static TitleProvider provideTitlesFromResource() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("films.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
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

    private static CacheProvider<String> createJedisCache(final JedisPool pool) {
        return new CacheProvider<>() {
            @Override
            public void put(String key, String value) {
                try (Jedis jedis = pool.getResource()) {
                    jedis.set(key, value);
                }
            }

            @Override
            public Optional<String> get(String key) {
                try (Jedis jedis = pool.getResource()) {
                    return Optional.ofNullable(jedis.get(key));
                }
            }
        };
    }

    private static JedisPool createJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(NUMBER_OF_THREADS);
        return new JedisPool(poolConfig, "172.17.0.2");
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FilmSuggestionsBenchmark.class.getSimpleName())
                .shouldDoGC(false)
                .build();

        new Runner(opt).run();
    }
}
