package pl.ciruk.whattowatch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

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
                .count();
        LOGGER.info("Number of suggestions: {}", numberOfFilms);
    }
}
