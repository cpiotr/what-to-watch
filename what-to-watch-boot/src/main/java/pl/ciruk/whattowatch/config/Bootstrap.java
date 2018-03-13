package pl.ciruk.whattowatch.config;

import lombok.extern.slf4j.Slf4j;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.suggest.FilmSuggestions;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@Slf4j
public class Bootstrap {
    private final FilmSuggestionProvider filmSuggestions;

    @Inject
    public Bootstrap(FilmSuggestionProvider filmSuggestions) {
        this.filmSuggestions = filmSuggestions;
    }

    @PostConstruct
    void onStartup() {
        log.info("Startup processing");

        long numberOfFilms = filmSuggestions.suggestFilms(1)
                .count();
        log.info("Number of suggestions: {}", numberOfFilms);
    }
}
