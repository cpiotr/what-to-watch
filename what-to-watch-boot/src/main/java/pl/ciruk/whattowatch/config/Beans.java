package pl.ciruk.whattowatch.config;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class Beans {

    @Value("${w2w.pool.size:16}")
    private Integer filmPoolSize;

    @Value("${w2w.titles.crawled.pages.limit:10}")
    private Integer titlesCrawledPagesLimit;

    @Bean
    ExecutorService executorService() {
        return Executors.newWorkStealingPool(filmPoolSize);
    }

    @Bean
    TitleProvider ekinoTitles(@Named("allCookiesHtml") HttpConnection httpConnection) {
        return new EkinoTitles(httpConnection, titlesCrawledPagesLimit);
    }

    @Bean
    FilmwebProxy filmwebProxy(@Named("noCookiesHtml") HttpConnection<Element> httpConnection) {
        return new FilmwebProxy(httpConnection);
    }

    @Bean
    DescriptionProvider filmwebDescriptions(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebDescriptions(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider imdbScores(@Named("noCookiesHtml") HttpConnection<Element> httpConnection, ExecutorService executorService) {
        return new ImdbWebScores(httpConnection, executorService);
    }

    @Bean
    ScoresProvider filmwebScores(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        return new FilmwebScores(filmwebProxy, executorService);
    }

    @Bean
    ScoresProvider metacriticScores(@Named("noCookiesHtml") HttpConnection<Element> httpConnection, ExecutorService executorService) {
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

    @PostConstruct
    private void logConfiguration() {
        log.info("Thread pool size: <{}>", filmPoolSize);
        log.info("Number of pages with titles to crawl: <{}>", titlesCrawledPagesLimit);
    }
}
