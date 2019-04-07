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
import pl.ciruk.whattowatch.utils.text.MissingValueException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

public class FilmwebReactiveDescriptionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutorService executorService;

    private final FilmwebProxy filmwebProxy;

    private final AtomicLong missingDescriptions = new AtomicLong();

    public FilmwebReactiveDescriptionProvider(FilmwebProxy filmwebProxy, ExecutorService executorService) {
        this.filmwebProxy = filmwebProxy;
        this.executorService = executorService;

        Metrics.gauge(
                Names.createName(DescriptionProvider.class, List.of("filmweb", "missing", "count")),
                Collections.emptyList(),
                missingDescriptions,
                AtomicLong::get
        );
    }

    public Flux<Description> findDescriptionBy(Title title) {
        LOGGER.debug("Title: {}", title);

        return Flux.fromStream(Stream.of(title.getOriginalTitle(), title.getLocalTitle()).filter(Objects::nonNull))
                .filter(not(String::isEmpty))
                .flatMap(titleText -> findFilmsForTitle(titleText, title.getYear()))
                .doOnNext(description -> description.foundFor(title));
    }

    private Mono<Description> findFilmsForTitle(String title, int year) {
        Runnable action = () -> LOGGER.warn("Could not get description for {} ({})", title, year);
        return filmwebProxy.monoSearchBy(title, year)
                .flux()
                .flatMap(element -> Flux.fromStream(FilmwebStreamSelectors.LINKS_FROM_SEARCH_RESULT.extractFrom(element)))
                .flatMap(href -> filmwebProxy.getMonoPageWithFilmDetailsFor(href).flux())
                .flatMap(this::extractDescriptionFrom)
                .next()
                .doOnSuccess(runIfEmpty(action));
    }

    private <T> Consumer<T> runIfEmpty(Runnable action) {
        return result -> {
            if (result == null) {
                action.run();
            }
        };
    }

    private Flux<Description> extractDescriptionFrom(Element pageWithDetails) {
        Element mainElement = pageWithDetails.selectFirst("div.filmMainHeader");
        var localTitle = Mono.justOrEmpty(FilmwebSelectors.LOCAL_TITLE.extractFrom(mainElement));
        var originalTitle = Mono.justOrEmpty(FilmwebSelectors.ORIGINAL_TITLE.extractFrom(mainElement));
        var extractedYear = Mono.justOrEmpty(extractYearFrom(mainElement));

        return localTitle.zipWith(originalTitle).zipWith(extractedYear)
                .map(titlesAndYear ->
                        Title.builder()
                                .title(titlesAndYear.getT1().getT1())
                                .originalTitle(titlesAndYear.getT1().getT2())
                                .year(titlesAndYear.getT2())
                                .build())
                .map(title -> Description.builder()
                        .title(title)
                        .poster(FilmwebSelectors.POSTER.extractFrom(pageWithDetails).orElse(""))
                        .plot(FilmwebSelectors.PLOT.extractFrom(mainElement).orElse(""))
                        .genres(FilmwebStreamSelectors.GENRES.extractFrom(mainElement).collect(toList()))
                        .build())
                .flux();
    }

    private Optional<Integer> extractYearFrom(Element details) {
        return FilmwebSelectors.YEAR.extractFrom(details)
                .map(this::parseYear);
    }

    private Integer parseYear(String extractFrom) {
        return Integer.valueOf(extractFrom);
    }
}

