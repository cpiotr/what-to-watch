package pl.ciruk.whattowatch;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.core.concurrent.Threads;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.CachedConnection;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.score.imdb.ImdbWebScores;
import pl.ciruk.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.suggest.FilmSuggestions;
import pl.ciruk.whattowatch.title.TitleProvider;
import pl.ciruk.whattowatch.title.ekino.EkinoTitles;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class WhatToWatchApplication {

    private static final int POOL_SIZE = 32;

    public static void main(String[] args) {
        Properties properties = loadDevProperties();
        ExecutorService threadPool = Executors.newWorkStealingPool(POOL_SIZE);
        Threads.setThreadNamePrefix("My-", threadPool);

        JedisPool jedisPool = createJedisPool(properties);
        CacheProvider<String> cache = createJedisCache(jedisPool);
        JsoupConnection connection = new JsoupConnection(new CachedConnection(cache, new HtmlConnection(OkHttpClient::new)));
        connection.init();

        FilmSuggestions suggestions = new FilmSuggestions(
                sampleTitleProvider(),
                sampleDescriptionProvider(threadPool, connection),
                sampleScoreProviders(threadPool, connection, new MetricRegistry()),
                threadPool);

        Stopwatch started = Stopwatch.createStarted();

        try {
            CompletableFutures.getAllOf(suggestions.suggestFilms())
                    .limit(100)
                    .filter(Film::isNotEmpty)
                    .forEach(System.out::println);
            started.stop();

            threadPool.shutdown();
            threadPool.awaitTermination(10, TimeUnit.SECONDS);

            System.out.println("Found in " + started.elapsed(TimeUnit.MILLISECONDS) + "ms");
        } catch (InterruptedException e) {
            log.error("main - Processing error", e);
        } finally {
            jedisPool.destroy();
        }
    }

    private static FilmwebDescriptions sampleDescriptionProvider(ExecutorService executorService, JsoupConnection connection) {
        return new FilmwebDescriptions(
                new FilmwebProxy(connection),
                new MetricRegistry(),
                executorService);
    }

    private static List<ScoresProvider> sampleScoreProviders(
            ExecutorService executorService,
            JsoupConnection connection,
            MetricRegistry metricRegistry) {
        return Lists.newArrayList(
                new FilmwebScores(new FilmwebProxy(connection), metricRegistry, executorService),
                new ImdbWebScores(connection, metricRegistry, executorService),
                new MetacriticScores(connection, metricRegistry, executorService)
        );
    }

    private static TitleProvider sampleTitleProvider() {
        HttpConnection<Element> keepCookiesConnection = createDirectConnectionWhichKeepsCookies();
        return new EkinoTitles(keepCookiesConnection, 10, new MetricRegistry());
    }

    private static HttpConnection<Element> createDirectConnectionWhichKeepsCookies() {
        Supplier<OkHttpClient> httpClientSupplier = () -> {
            OkHttpClient okHttpClient = new OkHttpClient();
            new AllCookies().applyTo(okHttpClient);
            return okHttpClient;
        };
        return new JsoupConnection(new HtmlConnection(httpClientSupplier));
    }

    private static CacheProvider<String> createJedisCache(final JedisPool pool) {
        return new CacheProvider<String>() {
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

    private static JedisPool createJedisPool(Properties properties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(POOL_SIZE);
        return new JedisPool(poolConfig, properties.getProperty("redis.host"));
    }

    private static Properties loadDevProperties() {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application-dev.properties"));
        } catch (Exception e) {
            log.error("loadDevProperties - Cannot load application-dev properties", e);
        }
        return properties;
    }

}
