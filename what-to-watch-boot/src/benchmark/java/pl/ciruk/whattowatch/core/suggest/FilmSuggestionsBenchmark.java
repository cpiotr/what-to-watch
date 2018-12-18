package pl.ciruk.whattowatch.core.suggest;

import com.google.common.cache.CacheBuilder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import pl.ciruk.whattowatch.boot.WhatToWatchApplication;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.core.score.imdb.ImdbScores;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScores;
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
@SuppressWarnings("PMD")
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

        workStealingPool = Executors.newWorkStealingPool(NUMBER_OF_THREADS);
        suggestionsWorkStealing = new FilmSuggestions(
                provideTitlesFromResource(),
                sampleDescriptionProvider(connection, workStealingPool),
                sampleScoreProviders(connection, workStealingPool),
                workStealingPool,
                CacheBuilder.newBuilder().build()
        );

        fixedPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        suggestionsFixedPool= new FilmSuggestions(
                provideTitlesFromResource(),
                sampleDescriptionProvider(connection, fixedPool),
                sampleScoreProviders(connection, fixedPool),
                fixedPool,
                CacheBuilder.newBuilder().build()
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

    private FilmwebDescriptions sampleDescriptionProvider(HttpConnection<String> htmlConnection, ExecutorService pool) {
        return new FilmwebDescriptions(
                new FilmwebProxy(new JsoupConnection(htmlConnection)),
                pool);
    }

    private static List<ScoresProvider> sampleScoreProviders(
            HttpConnection<String> connection,
            ExecutorService executorService) {
        JsoupConnection jsoupConnection = new JsoupConnection(connection);
        return List.of(
                new FilmwebScores(new FilmwebProxy(jsoupConnection), executorService),
                new MetacriticScores(jsoupConnection, executorService),
                new ImdbScores(jsoupConnection, executorService)
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
