package pl.ciruk.whattowatch.core.description.filmweb;

import io.micrometer.core.instrument.Metrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.text.MissingValueException;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

public class FilmwebDescriptionProvider implements DescriptionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutorService executorService;
    private final FilmwebProxy filmwebProxy;
    private final AtomicLong missingDescriptions = new AtomicLong();

    public FilmwebDescriptionProvider(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
        this.executorService = executorService;

        Metrics.gauge(
                Names.createName(DescriptionProvider.class, List.of("missing", "count")),
                List.of(Tags.getProviderTag("filmweb")),
                missingDescriptions,
                AtomicLong::get
        );
    }

    @Override
    public CompletableFuture<Optional<Description>> findDescriptionByAsync(Title title) {
        return CompletableFuture.supplyAsync(
                () -> findDescriptionBy(title),
                executorService
        ).exceptionally(t -> {
            LOGGER.error("Cannot get description of {}", title, t);
            return Optional.empty();
        });
    }

    @Override
    public Optional<Description> findDescriptionBy(Title title) {
        LOGGER.debug("Title: {}", title);

        var foundDescription = Stream.of(title.getOriginalTitle(), title.getLocalTitle())
                .filter(Objects::nonNull)
                .filter(not(String::isEmpty))
                .flatMap(t -> filmsForTitle(t, title.getYear()))
                .filter(description -> description.getTitle().matches(title))
                .peek(description -> description.foundFor(title))
                .findAny();
        if (foundDescription.isEmpty()) {
            LOGGER.warn("Missing description for: {}", title);
            missingDescriptions.incrementAndGet();
        }

        return foundDescription;
    }

    private Stream<Description> filmsForTitle(String title, int year) {
        var optionalResult = filmwebProxy.searchBy(title, year);

        return optionalResult.stream()
                .flatMap(FilmwebStreamSelectors.LINKS_FROM_SEARCH_RESULT::extractFrom)
                .map(filmwebProxy::getPageWithFilmDetailsFor)
                .flatMap(Optional::stream)
                .map(extractDescriptionOrElse(() -> LOGGER.warn("Could not get description for {} ({})", title, year)))
                .filter(not(Description::isEmpty));
    }

    private Function<Element, Description> extractDescriptionOrElse(Runnable actionIfMissing) {
        return pageWithDetails -> {
            try {
                return extractDescriptionFrom(pageWithDetails);
            } catch (MissingValueException e) {
                actionIfMissing.run();
                return Description.empty();
            }
        };
    }

    private Description extractDescriptionFrom(Element pageWithDetails) {
        Element mainElement = pageWithDetails.selectFirst("header.filmCoverSection__info");
        var localTitle = FilmwebSelectors.LOCAL_TITLE.extractFrom(mainElement)
                .orElseThrow(MissingValueException::new);
        var originalTitle = FilmwebSelectors.ORIGINAL_TITLE.extractFrom(mainElement)
                .orElse("");
        var extractedYear = extractYearFrom(mainElement)
                .orElseThrow(MissingValueException::new);

        var retrievedTitle = Title.builder()
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

