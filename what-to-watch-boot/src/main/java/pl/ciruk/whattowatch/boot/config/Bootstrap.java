package pl.ciruk.whattowatch.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

public class Bootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmSuggestionProvider filmSuggestions;

    public Bootstrap(FilmSuggestionProvider filmSuggestions) {
        this.filmSuggestions = filmSuggestions;
    }

    @PostConstruct
    protected void onStartup() {
        LOGGER.info("Startup processing");

        long numberOfFilms = filmSuggestions.suggestFilms(1)
                .map(CompletableFuture::join)
                .count();
        LOGGER.info("Preloaded {} film suggestions", numberOfFilms);
    }
}
