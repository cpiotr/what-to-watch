package pl.ciruk.whattowatch;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.score.imdb.IMDBScores;
import pl.ciruk.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.whattowatch.suggest.FilmSuggestions;
import pl.ciruk.whattowatch.title.zalukaj.ZalukajTitles;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WhatToWatchApplication {

	public static final int POOL_SIZE = 50;

	public static void main(String[] args) {
		Properties properties = loadDevProperties();

		ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);

		JedisPool pool = createJedisPool(properties);
		CacheProvider<String> cache = createJedisCache(pool);
		JsoupCachedConnection connection = new JsoupCachedConnection(cache, new OkHttpClient());

		FilmSuggestions suggestions = new FilmSuggestions(
				sampleTitleProvider(properties, executorService),
				sampleDescriptionProvider(executorService, connection),
				sampleScoreProviders(executorService, connection),
				executorService);

		Stopwatch started = Stopwatch.createStarted();
		try {
			suggestions.suggestFilms()
					.thenApply(s -> s.filter(Film::isWorthWatching))
					.thenAccept(s -> s.forEach(System.out::println))
					.get();
			started.stop();

			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.SECONDS);

			System.out.println("Found in " + started.elapsed(TimeUnit.MILLISECONDS) + "ms");
		} catch (InterruptedException | ExecutionException e) {
			log.error("main - Processing error", e);
		} finally {
			pool.destroy();
		}
	}

	private static FilmwebDescriptions sampleDescriptionProvider(ExecutorService executorService, JsoupCachedConnection connection) {
		return new FilmwebDescriptions(connection, executorService);
	}

	private static ArrayList<ScoresProvider> sampleScoreProviders(ExecutorService executorService, JsoupCachedConnection connection) {
		return Lists.newArrayList(
				new FilmwebScores(connection, executorService),
				new IMDBScores(connection, executorService),
				new MetacriticScores(connection, executorService)
		);
	}

	private static ZalukajTitles sampleTitleProvider(Properties properties, ExecutorService executorService) {
		JsoupCachedConnection keepCookiesConnection = createDirectConnectionWhichKeepsCookies();
		return new ZalukajTitles(
				keepCookiesConnection,
				executorService, properties.getProperty("zalukaj-login"),
				properties.getProperty("zalukaj-password"));
	}

	private static JsoupCachedConnection createDirectConnectionWhichKeepsCookies() {
		OkHttpClient httpClient = new OkHttpClient();
		new AllCookies().applyTo(httpClient);
		return new JsoupCachedConnection(CacheProvider.empty(), httpClient);
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
						return Optional.ofNullable(
								jedis.get(key));
					}
				}
			};
	}

	private static JedisPool createJedisPool(Properties properties) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(50);
		String maxActive = (String) properties.getOrDefault("redis.pool.maxActive", "8");
		poolConfig.setMaxIdle(
				Integer.valueOf(maxActive));
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
