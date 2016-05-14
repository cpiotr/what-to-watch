package pl.ciruk.whattowatch.description.filmweb;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.text.MissingValueException;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static pl.ciruk.core.stream.Predicates.not;

@Named
@Slf4j
public class FilmwebDescriptions implements DescriptionProvider {

	private final ExecutorService executorService;
	private final FilmwebProxy filmwebProxy;

	@Inject
	public FilmwebDescriptions(@Named FilmwebProxy filmwebProxy, ExecutorService executorService) {
		this.filmwebProxy = filmwebProxy;
		this.executorService = executorService;
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
		log.info("descriptionOf - Title: {}", title);

		return Stream.of(title.getTitle(), title.getOriginalTitle())
				.filter(t -> !isNullOrEmpty(t))
				.flatMap(t -> filmsForTitle(t, title.getYear()))
				.peek(d -> d.foundFor(title))
				.findAny();
	}

	Stream<Description> filmsForTitle(String title, int year) {
		Optional<Element> optionalResult = filmwebProxy.searchFor(title, year);

		return optionalResult
				.map(page -> FilmwebStreamSelectors.LINKS_FROM_SEARCH_RESULT.extractFrom(page)
								.map(link -> filmwebProxy.getPageWithFilmDetailsFor(link).get())
								.map(pageWithDetails -> {
									try {
										return extractDescriptionFrom(pageWithDetails);
									} catch (MissingValueException e) {
										log.warn("filmsForTitle - Could not get description for {} ({})", title, year);
										return Description.empty();
									}
								}).filter(not(Description::isEmpty)))
				.orElse(Stream.empty());
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

