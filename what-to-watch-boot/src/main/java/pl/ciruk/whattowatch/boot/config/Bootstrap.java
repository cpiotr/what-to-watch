package pl.ciruk.whattowatch.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.filter.FilmFilter;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

public class Bootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmSuggestionProvider filmSuggestions;
    private final FilmFilter filmFilter;

    protected Bootstrap(FilmSuggestionProvider filmSuggestions, FilmFilter filmFilter) {
        this.filmSuggestions = filmSuggestions;
        this.filmFilter = filmFilter;
    }

    @PostConstruct
    protected void onStartup() {
        LOGGER.info("Startup processing");

        long numberOfFilms = filmSuggestions.suggestFilms(1)
                .map(CompletableFuture::join)
                .filter(filmFilter::isWorthWatching)
                .count();
        LOGGER.info("Processed {} film suggestions", numberOfFilms);
    }
}
