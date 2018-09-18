package pl.ciruk.whattowatch.boot.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.core.suggest.Film;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.boot.cache.Cached;
import pl.ciruk.whattowatch.boot.cache.NotCached;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.core.score.imdb.ImdbWebScores;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScores;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestions;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.core.title.ekino.EkinoTitles;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class Beans {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("${w2w.pool.size:16}")
    private Integer filmPoolSize;

    @Value("${w2w.titles.pagesPerRequest:10}")
    private Integer titlePagesPerRequest;

    @Bean
    ExecutorService executorService() {
        String threadPrefix = "WhatToWatch";
        ExecutorService threadPoolExecutor = new ThreadPoolExecutor(
                filmPoolSize,
                filmPoolSize,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10_000),
                Threads.createThreadFactory(threadPrefix));

        return ExecutorServiceMetrics.monitor(Metrics.globalRegistry, threadPoolExecutor, threadPrefix);
    }

    @Bean
    TitleProvider ekinoTitles(@NotCached HttpConnection<Element> httpConnection) {
        return new EkinoTitles(httpConnection, titlePagesPerRequest);
    }

    @Bean
    FilmwebProxy filmwebProxy(@Cached HttpConnection<Element> httpConnection) {
        return new FilmwebProxy(httpConnection);
    }

    @Bean
    DescriptionProvider filmwebDescriptions(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebDescriptions(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider imdbScores(@Cached HttpConnection<Element> httpConnection, ExecutorService executorService) {
        return new ImdbWebScores(httpConnection, executorService);
    }

    @Bean
    ScoresProvider filmwebScores(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebScores(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider metacriticScores(@Cached HttpConnection<Element> httpConnection, ExecutorService executorService) {
        return new MetacriticScores(httpConnection, executorService);
    }

    @Bean
    Cache<Title, Film> cache() {
        Cache<Title, Film> filmCache = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(1000)
                .recordStats()
                .build();
        String className = FilmSuggestions.class.getSimpleName();
        Metrics.gauge(className + ".cache.size", List.of(), filmCache, Cache::size);
        Metrics.gauge(className + ".cache.hitCount", List.of(), filmCache, cache -> cache.stats().hitCount());
        Metrics.gauge(className + ".cache.requestCount", List.of(), filmCache, cache -> cache.stats().requestCount());
        return filmCache;
    }

    @Bean
    FilmSuggestionProvider filmSuggestions(
            TitleProvider titleProvider,
            DescriptionProvider descriptionProvider,
            List<ScoresProvider> scoreProviders,
            ExecutorService executorService,
            Cache<Title, Film> cache) {
        return new FilmSuggestions(titleProvider, descriptionProvider, scoreProviders, executorService, cache);
    }

    @Bean
    Bootstrap bootstrap(FilmSuggestionProvider filmSuggestionProvider) {
        return new Bootstrap(filmSuggestionProvider);
    }

    @PostConstruct
    private void logConfiguration() {
        LOGGER.info("Thread pool size: <{}>", filmPoolSize);
        LOGGER.info("Number of title pages crawled per request: <{}>", titlePagesPerRequest);
    }
}
