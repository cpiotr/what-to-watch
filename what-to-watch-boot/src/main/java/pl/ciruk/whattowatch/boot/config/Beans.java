package pl.ciruk.whattowatch.boot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.Cached;
import pl.ciruk.whattowatch.boot.cache.LongExpiry;
import pl.ciruk.whattowatch.boot.cache.NotCached;
import pl.ciruk.whattowatch.boot.cache.ShortExpiry;
import pl.ciruk.whattowatch.boot.config.Connections.NoCookies;
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
import pl.ciruk.whattowatch.neural.PersonalRatings;
import pl.ciruk.whattowatch.neural.RatedFilm;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

@Configuration
public class Beans {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final W2wProperties properties;

    public Beans(W2wProperties properties) {
        this.properties = properties;
        LOGGER.info("{}", properties);
    }

    @Bean
    ExecutorService executorService() {
        var threadPrefix = "WhatToWatch";
        return properties.pool().useVirtual() ? createVirtualPool(threadPrefix) : createForkJoinPool(threadPrefix);
    }

    private ExecutorService createForkJoinPool(String threadPrefix) {
        LOGGER.info("Creating fork join pool");
        ExecutorService threadPoolExecutor = new ForkJoinPool(
                properties.pool().size(),
                Threads.createForkJoinThreadFactory(threadPrefix),
                Threads.createUncaughtExceptionHandler(),
                true);
        return ExecutorServiceMetrics.monitor(Metrics.globalRegistry, threadPoolExecutor, threadPrefix);
    }

    private static ExecutorService createVirtualPool(String threadPrefix) {
        LOGGER.info("Creating pool of virtual threads");
        final var factory = Thread.ofVirtual()
                .name(threadPrefix + "-", 1)
                .uncaughtExceptionHandler(Threads.createUncaughtExceptionHandler())
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    // JustWatch takes POST requests, which aren't cached
    @Bean
    TitleProvider titleProvider(
            @Cached @ShortExpiry HttpConnection<Element> pageConnection,
            @NotCached @NoCookies HttpConnection<String> apiConnection) {
        var titles = properties.titles();
        return switch (titles.source()) {
            case JUSTWATCH -> new JustWatchTitleProvider(apiConnection, titles.streamingServices());
            case EKINO -> new EkinoTitleProvider(pageConnection, titles.pagesPerRequest());
        };
    }

    @Bean
    FilmwebProxy filmwebProxy(@Cached @LongExpiry HttpConnection<String> connection) {
        return new FilmwebProxy(connection);
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
    ScoresProvider metacriticScores(@Cached @LongExpiry HttpConnection<Element> httpConnection) {
        return new MetacriticScoresProvider(httpConnection);
    }

    @Bean
    Cache<Title, ScoredTitle> cache() {
        return Caffeine.newBuilder()
                .maximumSize(properties.films().cacheSize())
                .recordStats()
                .build();
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

    // IMDb's ratings aren't in the training data, so the rated films get them from the same index as suggestions
    @Bean
    PersonalRatings personalRatings(ScoresProvider imdbScores) {
        var ratedFilms = RatedFilm.readAll().stream()
                .map(ratedFilm -> ratedFilm.withScoresFrom(imdbScores))
                .toList();
        LOGGER.info("Learning my ratings from {} rated films", ratedFilms.size());
        return PersonalRatings.trainOn(ratedFilms);
    }

    @Bean
    FilmByScoreFilter filmByScoreFilter(PersonalRatings personalRatings) {
        return new FilmByScoreFilter(
                scored -> Objects.requireNonNullElse(personalRatings.predict(scored), 0.0),
                properties.suggestions().ratingThreshold());
    }

    @Bean
    Predicate<ScoredTitle> atLeastOneCriticScore() {
        return scored -> scored.scores().stream().anyMatch(score -> score.type() == ScoreType.CRITIC);
    }

    @Bean
    SuggestionFilter suggestionFilter(List<Predicate<ScoredTitle>> filters) {
        return new SuggestionFilter(filters);
    }

}
