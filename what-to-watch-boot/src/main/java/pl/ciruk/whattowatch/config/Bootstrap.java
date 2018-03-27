package pl.ciruk.whattowatch.config;

import lombok.extern.slf4j.Slf4j;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;

import javax.annotation.PostConstruct;

@Slf4j
public class Bootstrap {
    private final FilmSuggestionProvider filmSuggestions;

    public Bootstrap(FilmSuggestionProvider filmSuggestions) {
        this.filmSuggestions = filmSuggestions;
    }

    @PostConstruct
    protected void onStartup() {
        log.info("Startup processing");

        long numberOfFilms = filmSuggestions.suggestFilms(1)
                .count();
        log.info("Number of suggestions: {}", numberOfFilms);
    }
}
