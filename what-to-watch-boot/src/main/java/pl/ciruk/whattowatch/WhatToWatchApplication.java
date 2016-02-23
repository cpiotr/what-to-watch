package pl.ciruk.whattowatch;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.CachedConnection;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.core.net.json.JsonConnection;
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

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class WhatToWatchApplication {

	public static final int POOL_SIZE = 32;

	public static void main(String[] args) {
		Properties properties = loadDevProperties();
		ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);

		JedisPool pool = createJedisPool(properties);
		CacheProvider<String> cache = createJedisCache(pool);
		JsoupConnection connection = new JsoupConnection(new CachedConnection(cache, new HtmlConnection(new OkHttpClient())));
		JsonConnection jsonConnection = new JsonConnection(new CachedConnection(cache, new HtmlConnection(new OkHttpClient())));

		FilmSuggestions suggestions = new FilmSuggestions(
				sampleTitleProvider(properties, executorService),
				sampleDescriptionProvider(executorService, connection),
				sampleScoreProviders(executorService, connection, jsonConnection),
				executorService);

		Stopwatch started = Stopwatch.createStarted();

		try {
			List<Film> films = suggestions.suggestFilms()
					.map(CompletableFutures::get)
					.collect(Collectors.toList());
			films.stream()
					.filter(Film::isNotEmpty)
					.forEach(System.out::println);
			started.stop();

			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.SECONDS);

			System.out.println("Found in " + started.elapsed(TimeUnit.MILLISECONDS) + "ms");
		} catch (InterruptedException e) {
			log.error("main - Processing error", e);
		} finally {
			pool.destroy();
		}
	}

	private static FilmwebDescriptions sampleDescriptionProvider(ExecutorService executorService, JsoupConnection connection) {
		return new FilmwebDescriptions(connection, executorService);
	}

	private static List<ScoresProvider> sampleScoreProviders(ExecutorService executorService, JsoupConnection connection, JsonConnection jsonConnection) {
		return Lists.newArrayList(
				new FilmwebScores(connection, executorService),
				new IMDBScores(jsonConnection, executorService),
				new MetacriticScores(connection, executorService)
		);
	}

	private static ZalukajTitles sampleTitleProvider(Properties properties, ExecutorService executorService) {
		HttpConnection<Element> keepCookiesConnection = createDirectConnectionWhichKeepsCookies();
		return new ZalukajTitles(
				keepCookiesConnection,
				properties.getProperty("zalukaj-login"),
				properties.getProperty("zalukaj-password"));
	}

	private static HttpConnection<Element> createDirectConnectionWhichKeepsCookies() {
		OkHttpClient httpClient = new OkHttpClient();
		new AllCookies().applyTo(httpClient);
		return new JsoupConnection(new HtmlConnection(httpClient));
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
