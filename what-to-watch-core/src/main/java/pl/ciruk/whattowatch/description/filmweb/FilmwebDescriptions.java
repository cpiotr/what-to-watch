package pl.ciruk.whattowatch.description.filmweb;

import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.text.MissingValueException;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static pl.ciruk.core.stream.Predicates.not;

@Slf4j
public class FilmwebDescriptions implements DescriptionProvider {

    private final ExecutorService executorService;

    private final FilmwebProxy filmwebProxy;

    private final AtomicLong missingDescriptions = new AtomicLong();

    public FilmwebDescriptions(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
        this.executorService = executorService;

        Metrics.gauge(
                FilmwebDescriptions.class.getSimpleName() + ".missingDescriptions",
                Collections.emptyList(),
                missingDescriptions,
                AtomicLong::get
        );
    }

    @Override
    public CompletableFuture<Optional<Description>> descriptionOfAsync(Title title) {
        return CompletableFuture.supplyAsync(
                () -> descriptionOf(title),
                executorService
        ).exceptionally(t -> {
            log.error("Cannot get description of {}", title, t);
            return Optional.empty();
        });
    }

    @Override
    public Optional<Description> descriptionOf(Title title) {
        log.debug("descriptionOf - Title: {}", title);

        Optional<Description> foundDescription = Stream.of(title.getOriginalTitle(), title.getTitle())
                .filter(Objects::nonNull)
                .filter(not(String::isEmpty))
                .flatMap(t -> filmsForTitle(t, title.getYear()))
                .peek(description -> description.foundFor(title))
                .findAny();
        if (!foundDescription.isPresent()) {
            log.warn("descriptionOf - Missing description for: {}", title);
            missingDescriptions.incrementAndGet();
        }

        return foundDescription;
    }

    private Stream<Description> filmsForTitle(String title, int year) {
        Optional<Element> optionalResult = filmwebProxy.searchFor(title, year);

        return optionalResult.stream()
                .flatMap(FilmwebStreamSelectors.LINKS_FROM_SEARCH_RESULT::extractFrom)
                .map(filmwebProxy::getPageWithFilmDetailsFor)
                .flatMap(Optional::stream)
                .map(pageWithDetails -> {
                    try {
                        return extractDescriptionFrom(pageWithDetails);
                    } catch (MissingValueException e) {
                        log.warn("filmsForTitle - Could not get description for {} ({})", title, year);
                        return Description.empty();
                    }
                }).filter(not(Description::isEmpty));
    }

    private Description extractDescriptionFrom(Element pageWithDetails) {
        String localTitle = FilmwebSelectors.LOCAL_TITLE.extractFrom(pageWithDetails)
                .orElseThrow(MissingValueException::new);
        String originalTitle = FilmwebSelectors.ORIGINAL_TITLE.extractFrom(pageWithDetails)
                .orElse("");
        int extractedYear = extractYearFrom(pageWithDetails)
                .orElseThrow(MissingValueException::new);

        Title retrievedTitle = Title.builder()
                .title(localTitle)
                .originalTitle(originalTitle)
                .year(extractedYear)
                .build();

        return Description.builder()
                .title(retrievedTitle)
                .poster(FilmwebSelectors.POSTER.extractFrom(pageWithDetails).orElse(""))
                .plot(FilmwebSelectors.PLOT.extractFrom(pageWithDetails).orElse(""))
                .genres(FilmwebStreamSelectors.GENRES.extractFrom(pageWithDetails).collect(toList()))
                .build();
    }

    private Optional<Integer> extractYearFrom(Element details) {
        return FilmwebSelectors.YEAR.extractFrom(details)
                .map(this::parseYear);
    }

    private Integer parseYear(String extractFrom) {
        return Integer.valueOf(extractFrom);
    }
}

