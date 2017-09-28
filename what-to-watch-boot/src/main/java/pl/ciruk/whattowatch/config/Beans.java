package pl.ciruk.whattowatch.config;

import com.codahale.metrics.MetricRegistry;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        return new ThreadPoolExecutor(
                filmPoolSize,
                filmPoolSize,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10_000),
                Threads.createThreadFactory(threadPrefix));
    }

    @Bean
    TitleProvider ekinoTitles(
            @Named("allCookiesHtml") HttpConnection<Element> httpConnection,
            MetricRegistry metricRegistry) {
        return new EkinoTitles(httpConnection, titlePagesPerRequest, metricRegistry);
    }

    @Bean
    FilmwebProxy filmwebProxy(@Named("noCookiesHtml") HttpConnection<Element> httpConnection) {
        return new FilmwebProxy(httpConnection);
    }

    @Bean
    DescriptionProvider filmwebDescriptions(
            FilmwebProxy filmwebProxy,
            MetricRegistry metricRegistry,
            ExecutorService executorService) {
        return new FilmwebDescriptions(filmwebProxy, metricRegistry, executorService);
    }

    @Bean
    ScoresProvider imdbScores(
            @Named("noCookiesHtml") HttpConnection<Element> httpConnection,
            MetricRegistry metricRegistry,
            ExecutorService executorService) {
        return new ImdbWebScores(httpConnection, metricRegistry, executorService);
    }

    @Bean
    ScoresProvider filmwebScores(
            FilmwebProxy filmwebProxy,
            MetricRegistry metricRegistry,
            ExecutorService executorService) {
        return new FilmwebScores(filmwebProxy, metricRegistry, executorService);
    }

    @Bean
    ScoresProvider metacriticScores(
            @Named("noCookiesHtml") HttpConnection<Element> httpConnection,
            MetricRegistry metricRegistry,
            ExecutorService executorService) {
        return new MetacriticScores(httpConnection, metricRegistry, executorService);
    }

    @Bean
    FilmSuggestionProvider filmSuggestions(
            TitleProvider titleProvider,
            DescriptionProvider descriptionProvider,
            List<ScoresProvider> scoreProviders,
            ExecutorService executorService) {
        return new FilmSuggestions(titleProvider, descriptionProvider, scoreProviders, executorService);
    }

    @PostConstruct
    private void logConfiguration() {
        log.info("Thread pool size: <{}>", filmPoolSize);
        log.info("Number of title pages crawled per request: <{}>", titlePagesPerRequest);
    }
}
