package pl.ciruk.whattowatch.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import pl.ciruk.whattowatch.core.filter.FilmFilter;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;

import java.lang.invoke.MethodHandles;

public class Bootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FilmSuggestionProvider filmSuggestions;
    private final FilmFilter filmFilter;

    Bootstrap(FilmSuggestionProvider filmSuggestions, FilmFilter filmFilter) {
        this.filmSuggestions = filmSuggestions;
        this.filmFilter = filmFilter;
    }

    // Crawl the first page in the background, so the app serves requests while the caches fill
    @EventListener(ApplicationReadyEvent.class)
    public void startWarmingUpCaches() {
        Thread.ofPlatform()
                .name("Bootstrap")
                .daemon()
                .start(this::warmUpCaches);
    }

    private void warmUpCaches() {
        LOGGER.info("Startup processing");
        try {
            // Every search starts before any is awaited, so the rate-limited sites work at the same time
            long numberOfFilms = CompletableFutures.getAllOf(filmSuggestions.suggestFilms(1))
                    .filter(filmFilter::isWorthWatching)
                    .distinct()
                    .count();
            LOGGER.info("Processed {} film suggestions", numberOfFilms);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not warm up caches", e);
        }
    }
}
