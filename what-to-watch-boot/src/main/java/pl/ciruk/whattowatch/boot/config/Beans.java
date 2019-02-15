package pl.ciruk.whattowatch.boot.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.Cached;
import pl.ciruk.whattowatch.boot.cache.NotCached;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptions;
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
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.core.title.ekino.EkinoTitleProvider;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static pl.ciruk.whattowatch.boot.config.Configs.logConfigurationEntry;

@Configuration
@SuppressWarnings("PMD.TooManyMethods")
public class Beans {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Integer filmPoolSize;
    private final Integer titlePagesPerRequest;
    private final Double filmScoreThreshold;

    public Beans(
            @Value("${w2w.pool.size:16}") Integer filmPoolSize,
            @Value("${w2w.titles.pagesPerRequest:10}") Integer titlePagesPerRequest,
            @Value("${w2w.suggestions.filter.scoreThreshold:0.6}") Double filmScoreThreshold) {
        this.filmPoolSize = filmPoolSize;
        this.titlePagesPerRequest = titlePagesPerRequest;
        this.filmScoreThreshold = filmScoreThreshold;
    }

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
        return new EkinoTitleProvider(httpConnection, titlePagesPerRequest);
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
        return new ImdbScoresProvider(httpConnection, executorService);
    }

    @Bean
    ScoresProvider filmwebScores(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebScoresProvider(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider metacriticScores(@Cached HttpConnection<Element> httpConnection, ExecutorService executorService) {
        return new MetacriticScoresProvider(httpConnection, executorService);
    }

    @Bean
    FilmSuggestionProvider filmSuggestions(
            TitleProvider titleProvider,
            DescriptionProvider descriptionProvider,
            List<ScoresProvider> scoreProviders,
            ExecutorService executorService) {
        return new FilmSuggestionProvider(
                titleProvider,
                descriptionProvider,
                scoreProviders,
                executorService,
                new ConcurrentHashMap<>());
    }

    @Bean
    Bootstrap bootstrap(FilmSuggestionProvider filmSuggestionProvider) {
        return new Bootstrap(filmSuggestionProvider);
    }

    @Bean
    FilmByScoreFilter filmByScoreFilter() {
        return new FilmByScoreFilter(filmScoreThreshold);
    }

    @Bean
    Predicate<Film> atLeastOneCriticScore() {
        return film -> film.getScores().stream().anyMatch(score -> score.getType().equals(ScoreType.CRITIC));
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
    }
}
