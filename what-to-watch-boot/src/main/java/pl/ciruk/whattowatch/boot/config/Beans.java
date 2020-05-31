package pl.ciruk.whattowatch.boot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.Cached;
import pl.ciruk.whattowatch.boot.cache.LongExpiry;
import pl.ciruk.whattowatch.boot.cache.ShortExpiry;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptionProvider;
import pl.ciruk.whattowatch.core.filter.FilmByScoreFilter;
import pl.ciruk.whattowatch.core.filter.FilmFilter;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScoresProvider;
import pl.ciruk.whattowatch.core.score.imdb.ImdbScoresProvider;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.suggest.Film;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.core.title.onetwothree.OneTwoThreeTitleProvider;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

import static pl.ciruk.whattowatch.boot.config.Configs.logConfigurationEntry;

@Configuration
@SuppressWarnings("PMD.TooManyMethods")
public class Beans {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Integer filmPoolSize;
    private final Integer titlePagesPerRequest;
    private final Double filmScoreThreshold;
    private final Integer filmCacheSize;

    public Beans(
            @Value("${w2w.pool.size:16}") Integer filmPoolSize,
            @Value("${w2w.titles.pagesPerRequest:10}") Integer titlePagesPerRequest,
            @Value("${w2w.suggestions.filter.scoreThreshold:0.6}") Double filmScoreThreshold,
            @Value("${w2w.films.cache.size:10000}") Integer filmCacheSize) {
        this.filmPoolSize = filmPoolSize;
        this.titlePagesPerRequest = titlePagesPerRequest;
        this.filmScoreThreshold = filmScoreThreshold;
        this.filmCacheSize = filmCacheSize;
    }

    @Bean
    ExecutorService executorService() {
        var threadPrefix = "WhatToWatch";
        ExecutorService threadPoolExecutor = new ForkJoinPool(
                filmPoolSize,
                Threads.createForkJoinThreadFactory(threadPrefix),
                Threads.createUncaughtExceptionHandler(),
                true);
        return ExecutorServiceMetrics.monitor(Metrics.globalRegistry, threadPoolExecutor, threadPrefix);
    }

    @Bean
    TitleProvider titleProvider(@Cached @ShortExpiry HttpConnection<Element> httpConnection) {
        return new OneTwoThreeTitleProvider(httpConnection, titlePagesPerRequest);
    }

    @Bean
    FilmwebProxy filmwebProxy(@Cached @LongExpiry HttpConnection<Element> httpConnection) {
        return new FilmwebProxy(httpConnection);
    }

    @Bean
    DescriptionProvider filmwebDescriptions(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebDescriptionProvider(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider imdbScores(@Cached @LongExpiry HttpConnection<Element> httpConnection, ExecutorService executorService) {
        return new ImdbScoresProvider(httpConnection, executorService);
    }

    @Bean
    ScoresProvider filmwebScores(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebScoresProvider(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider metacriticScores(@Cached @LongExpiry HttpConnection<Element> httpConnection, ExecutorService executorService) {
        return new MetacriticScoresProvider(httpConnection, executorService);
    }

    @Bean
    Cache<Title, Film> cache() {
        return Caffeine.newBuilder()
                .maximumSize(filmCacheSize)
                .recordStats()
                .build();
    }

    @Bean
    FilmSuggestionProvider filmSuggestions(
            TitleProvider titleProvider,
            DescriptionProvider descriptionProvider,
            List<ScoresProvider> scoreProviders,
            ExecutorService executorService,
            Cache<Title, Film> cache) {
        return new FilmSuggestionProvider(titleProvider, descriptionProvider, scoreProviders, executorService, cache);
    }

    @Bean
    Bootstrap bootstrap(FilmSuggestionProvider filmSuggestionProvider, FilmFilter filmFilter) {
        return new Bootstrap(filmSuggestionProvider, filmFilter);
    }

    @Bean
    FilmByScoreFilter filmByScoreFilter() {
        return new FilmByScoreFilter(filmScoreThreshold);
    }

    @Bean
    Predicate<Film> atLeastOneCriticScore() {
        return film -> film.scores().stream().anyMatch(score -> score.type().equals(ScoreType.CRITIC));
    }

    @Bean
    FilmFilter filmFilter(List<Predicate<Film>> filters) {
        return new FilmFilter(filters);
    }

    @PostConstruct
    void logConfiguration() {
        logConfigurationEntry(LOGGER, "Thread pool size", filmPoolSize);
        logConfigurationEntry(LOGGER, "Number of title pages crawled per request", titlePagesPerRequest);
        logConfigurationEntry(LOGGER, "Film score threshold", filmScoreThreshold);
        logConfigurationEntry(LOGGER, "Film cache size", filmCacheSize);
    }
}
