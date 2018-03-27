package pl.ciruk.whattowatch.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.core.concurrent.Threads;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.score.imdb.ImdbWebScores;
import pl.ciruk.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.suggest.FilmSuggestions;
import pl.ciruk.whattowatch.title.TitleProvider;
import pl.ciruk.whattowatch.title.ekino.EkinoTitles;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.*;

@Configuration
@Slf4j
public class Beans {

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
    FilmSuggestionProvider filmSuggestions(
            TitleProvider titleProvider,
            DescriptionProvider descriptionProvider,
            List<ScoresProvider> scoreProviders,
            ExecutorService executorService) {
        return new FilmSuggestions(titleProvider, descriptionProvider, scoreProviders, executorService);
    }

    @Bean
    Bootstrap bootstrap(FilmSuggestionProvider filmSuggestionProvider) {
        return new Bootstrap(filmSuggestionProvider);
    }

    @PostConstruct
    private void logConfiguration() {
        log.info("Thread pool size: <{}>", filmPoolSize);
        log.info("Number of title pages crawled per request: <{}>", titlePagesPerRequest);
    }
}
