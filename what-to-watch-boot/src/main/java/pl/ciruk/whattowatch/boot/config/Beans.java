package pl.ciruk.whattowatch.boot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import net.jodah.failsafe.CircuitBreaker;
import okhttp3.OkHttpClient;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.RedisCache;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptionProvider;
import pl.ciruk.whattowatch.core.filter.FilmByScoreFilter;
import pl.ciruk.whattowatch.core.filter.SuggestionFilter;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScoresProvider;
import pl.ciruk.whattowatch.core.score.imdb.ImdbDatasetScoresProvider;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.suggest.ScoredTitle;
import pl.ciruk.whattowatch.core.suggest.SuggestionProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.core.title.ekino.EkinoTitleProvider;
import pl.ciruk.whattowatch.core.title.justwatch.JustWatchTitleProvider;
import pl.ciruk.whattowatch.ratings.MyRatings;
import pl.ciruk.whattowatch.ratings.PersonalRatings;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.net.CachedConnection;
import pl.ciruk.whattowatch.utils.net.DomainRateLimiter;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.net.InMemoryCookieJar;
import pl.ciruk.whattowatch.utils.net.OkHttpConnection;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;
import redis.clients.jedis.RedisClient;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

@Configuration
public class Beans {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final W2wProperties properties;

    public Beans(W2wProperties properties) {
        this.properties = properties;
        LOGGER.info("{}", properties);
    }

    // Tasks mostly wait for their turn at a rate-limited site, so a thread each costs next to nothing
    @Bean
    ExecutorService executorService() {
        var factory = Thread.ofVirtual()
                .name("WhatToWatch-", 1)
                .uncaughtExceptionHandler(Threads.createUncaughtExceptionHandler())
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    @Bean
    TitleProvider titleProvider(
            OkHttpClient httpClient,
            DomainRateLimiter rateLimiter,
            RedisClient redisClient,
            CircuitBreaker<Optional<String>> circuitBreaker) {
        var titles = properties.titles();
        return switch (titles.source()) {
            // JustWatch takes POST requests, which aren't cached
            case JUSTWATCH -> new JustWatchTitleProvider(
                    new OkHttpConnection(httpClient, rateLimiter),
                    titles.streamingServices());
            case EKINO -> new EkinoTitleProvider(
                    ekinoPages(httpClient, rateLimiter, redisClient, circuitBreaker),
                    titles.pagesPerRequest());
        };
    }

    // Only Ekino keeps the cookies a site sets, and its pages are cached for w2w.cache.short-expiry
    private HttpConnection<Element> ekinoPages(
            OkHttpClient httpClient,
            DomainRateLimiter rateLimiter,
            RedisClient redisClient,
            CircuitBreaker<Optional<String>> circuitBreaker) {
        var withCookies = httpClient.newBuilder().cookieJar(new InMemoryCookieJar()).build();
        var cache = new RedisCache(redisClient, "short", properties.cache().shortExpiry(), circuitBreaker);
        return new JsoupConnection(new CachedConnection(cache, new OkHttpConnection(withCookies, rateLimiter)));
    }

    @Bean
    FilmwebProxy filmwebProxy(HttpConnection<String> cachedPages) {
        return new FilmwebProxy(cachedPages);
    }

    @Bean
    DescriptionProvider filmwebDescriptions(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebDescriptionProvider(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider imdbScores() {
        return new ImdbDatasetScoresProvider(properties.imdb().datasetsDirectory());
    }

    @Bean
    ScoresProvider filmwebScores(FilmwebProxy filmwebProxy) {
        return new FilmwebScoresProvider(filmwebProxy);
    }

    @Bean
    ScoresProvider metacriticScores(HttpConnection<String> cachedPages) {
        return new MetacriticScoresProvider(new JsoupConnection(cachedPages));
    }

    @Bean
    Cache<Title, ScoredTitle> cache() {
        Cache<Title, ScoredTitle> cache = Caffeine.newBuilder()
                .maximumSize(properties.films().cacheSize())
                .recordStats()
                .build();
        return CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache, "scoredTitles");
    }

    @Bean
    SuggestionProvider suggestionProvider(
            TitleProvider titleProvider,
            DescriptionProvider descriptionProvider,
            List<ScoresProvider> scoreProviders,
            ExecutorService executorService,
            Cache<Title, ScoredTitle> cache) {
        return new SuggestionProvider(titleProvider, descriptionProvider, scoreProviders, executorService, cache);
    }

    @Bean
    @ConditionalOnProperty(name = "w2w.bootstrap.enabled", matchIfMissing = true)
    Bootstrap bootstrap(SuggestionProvider suggestionProvider, SuggestionFilter suggestionFilter) {
        return new Bootstrap(suggestionProvider, suggestionFilter);
    }

    // IMDb's ratings aren't in the training data, so the rated titles get them from the same index as suggestions.
    // Learning again after every rating takes milliseconds.
    @Bean
    MyRatings myRatings(ScoresProvider imdbScores) {
        var file = properties.ratings().file().toAbsolutePath();
        return MyRatings.open(file, ratedTitles -> {
            LOGGER.info("Learning my ratings from {} rated titles in {}", ratedTitles.size(), file);
            return PersonalRatings.trainOn(ratedTitles.stream()
                    .map(ratedTitle -> ratedTitle.withScoresFrom(imdbScores))
                    .toList());
        });
    }

    // Listed here rather than collected from the context, where any other predicate would become a filter too
    @Bean
    SuggestionFilter suggestionFilter(MyRatings myRatings) {
        var predictedForMe = new FilmByScoreFilter(
                scored -> Objects.requireNonNullElse(myRatings.predict(scored), 0.0),
                properties.suggestions().ratingThreshold());
        Predicate<ScoredTitle> atLeastOneCriticScore =
                scored -> scored.scores().stream().anyMatch(score -> score.type() == ScoreType.CRITIC);
        return new SuggestionFilter(List.of(predictedForMe, atLeastOneCriticScore));
    }

}
