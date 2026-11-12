package pl.ciruk.whattowatch.core.description.filmweb;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class FilmwebDescriptionProvider implements DescriptionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutorService executorService;
    private final FilmwebProxy filmwebProxy;
    private final Counter missingDescriptions = Metrics.counter(
            Names.createName(DescriptionProvider.class, List.of("missing", "count")),
            List.of(Tags.getProviderTag("filmweb")));
    private final FilmwebSelectors selector = new FilmwebSelectors();

    public FilmwebDescriptionProvider(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
        this.executorService = executorService;
    }

    @Override
    public Optional<Description> findDescriptionBy(Title title) {
        LOGGER.debug("Title: {}", title);

        var foundDescription = Stream.of(title.originalTitle(), title.localTitle())
                .filter(Objects::nonNull)
                .filter(not(String::isEmpty))
                .flatMap(text -> filmsForTitle(text, title))
                .filter(description -> description.getTitle().matches(title))
                .findAny();
        if (foundDescription.isEmpty()) {
            LOGGER.warn("Missing description for: {}", title);
            missingDescriptions.increment();
        }

        return foundDescription;
    }

    private Stream<Description> filmsForTitle(String titleText, Title requestedTitle) {
        // Filmweb can rank similarly named films first, such as "The Real Casino Royale"
        var futures = filmwebProxy.searchBy(titleText, requestedTitle.year())
                .filter(film -> film.toTitle().matches(requestedTitle))
                .limit(3)
                .map(film -> CompletableFuture.supplyAsync(() -> filmwebProxy.getPageWithFilmDetailsFor(film), executorService))
                .toList();
        return CompletableFutures.getAllOf(futures)
                .flatMap(Optional::stream)
                .map(page -> descriptionFrom(page, requestedTitle))
                .flatMap(Optional::stream);
    }

    private Optional<Description> descriptionFrom(Element page, Title requestedTitle) {
        var details = page.selectFirst(".filmCoverSection__titleDetails");
        var localTitle = Optional.ofNullable(details).flatMap(selector::findLocalTitle);
        var year = Optional.ofNullable(details).flatMap(selector::findYear);
        if (localTitle.isEmpty() || year.isEmpty()) {
            LOGGER.warn("Could not read the title and year of a film found for {}", requestedTitle);
            return Optional.empty();
        }

        var foundTitle = Title.builder()
                .title(localTitle.get())
                .originalTitle(selector.findOriginalTitle(details).orElse(""))
                .year(year.get())
                .build();
        return Optional.of(Description.builder()
                .title(foundTitle)
                .foundFor(requestedTitle)
                .poster(selector.findPoster(page).orElse(""))
                .plot(selector.findPlot(page).orElse(""))
                .genres(selector.findGenres(page).toList())
                .build());
    }
}
