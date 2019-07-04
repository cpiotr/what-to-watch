package pl.ciruk.whattowatch.boot;

import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptionProvider;
import pl.ciruk.whattowatch.core.filter.FilmByScoreFilter;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScoresProvider;
import pl.ciruk.whattowatch.core.score.imdb.ImdbScoresProvider;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.suggest.Film;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.core.title.onetwothree.OneTwoThreeTitleProvider;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.net.CachedConnection;
import pl.ciruk.whattowatch.utils.net.HtmlConnection;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("PMD")
public class WhatToWatchApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int POOL_SIZE = 32;

    public static void main(String[] args) {
        Properties properties = loadDevProperties();
        ExecutorService threadPool = Executors.newWorkStealingPool(POOL_SIZE);
        Threads.setThreadNamePrefix("My", threadPool);

        JedisPool jedisPool = createJedisPool(properties);
        CacheProvider<String> cache = createJedisCache(jedisPool);
        JsoupConnection connection = createJsoupConnection(cache);

        FilmSuggestionProvider suggestions = new FilmSuggestionProvider(
                sampleTitleProvider(connection),
                sampleDescriptionProvider(threadPool, connection),
                sampleScoreProviders(threadPool, connection),
                threadPool,
                Caffeine.newBuilder().build());
        FilmByScoreFilter scoreFilter = new FilmByScoreFilter(0.65);

        long startTime = System.currentTimeMillis();

        try {
            CompletableFutures.getAllOf(suggestions.suggestFilms(1))
                    .limit(100)
                    .filter(Film::isNotEmpty)
                    .filter(scoreFilter)
                    .forEach(System.out::println);
            long stopTime = System.currentTimeMillis();

            threadPool.shutdown();
            threadPool.awaitTermination(10, TimeUnit.SECONDS);

            System.out.println("Found in " + (stopTime - startTime) + "ms");
        } catch (InterruptedException e) {
            LOGGER.error("Processing error", e);
        } finally {
            jedisPool.destroy();
        }
    }

    public static HtmlConnection createHttpConnection() {
        return new HtmlConnection(createHttpClient());
    }

    private static JsoupConnection createJsoupConnection(CacheProvider<String> cache) {
        return new JsoupConnection(new CachedConnection(cache, createHttpConnection()));
    }

    private static OkHttpClient createHttpClient() {
        ConnectionPool connectionPool = new ConnectionPool(32, 12_000, TimeUnit.SECONDS);
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .build();
    }

    private static FilmwebDescriptionProvider sampleDescriptionProvider(ExecutorService executorService, JsoupConnection connection) {
        return new FilmwebDescriptionProvider(
                new FilmwebProxy(connection),
                executorService);
    }

    private static List<ScoresProvider> sampleScoreProviders(
            ExecutorService executorService,
            JsoupConnection connection) {
        return List.of(
                new FilmwebScoresProvider(new FilmwebProxy(connection), executorService),
                new ImdbScoresProvider(connection, executorService),
                new MetacriticScoresProvider(connection, executorService)
        );
    }

    private static TitleProvider sampleTitleProvider(JsoupConnection detailsConnection) {
        HttpConnection<Element> keepCookiesConnection = createDirectConnectionWhichKeepsCookies();
        return new OneTwoThreeTitleProvider(keepCookiesConnection, detailsConnection, 20);
    }

    private static HttpConnection<Element> createDirectConnectionWhichKeepsCookies() {
        return new JsoupConnection(createHttpConnection());
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
        };
    }

    private static JedisPool createJedisPool(Properties properties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(POOL_SIZE * 2);
        return new JedisPool(poolConfig, properties.getProperty("redis.host"));
    }

    private static Properties loadDevProperties() {
        Properties properties = new Properties();
        try {
            InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application-dev.properties");
            if (resourceAsStream != null) {
                properties.load(resourceAsStream);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot load application-dev properties", e);
        }
        return properties;
    }

}
