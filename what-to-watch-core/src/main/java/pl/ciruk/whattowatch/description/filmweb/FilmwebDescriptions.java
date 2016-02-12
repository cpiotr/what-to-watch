package pl.ciruk.whattowatch.description.filmweb;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.core.text.MissingValueException;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.title.Title;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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

	private final JsoupConnection connection;

	private final ExecutorService executorService;

	@Inject
	public FilmwebDescriptions(@Named("noCookies") JsoupConnection connection, ExecutorService executorService) {
		this.connection = connection;
		this.executorService = executorService;
	}

	@Override
	public CompletableFuture<Optional<Description>> descriptionOfAsync(Title title) {
		return CompletableFuture.supplyAsync(
				() -> descriptionOf(title),
				executorService
		);
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
		Optional<Element> optionalResult = searchFor(title, year);

		return optionalResult
				.map(page -> FilmwebStreamSelectors.LINKS_FROM_SEARCH_RESULT.extractFrom(page)
								.map(this::getPageWithFilmDetailsFor)
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

	private Element getPageWithFilmDetailsFor(String href) {
		return connection.connectToAndGet(FilmwebSelectors.ROOT_URL + href).get();
	}

	Optional<Element> searchFor(String title, int year) {
		String url = null;
		try {
			url = String.format("%s/search/film?q=%s&startYear=%d&endYear=%d",
					FilmwebSelectors.ROOT_URL,
					URLEncoder.encode(title, Charset.defaultCharset().name()),
					year,
					year);

		} catch (UnsupportedEncodingException e) {
			log.warn("searchFor - Could not find films for title={} year={}", title, year, e);
		}

		return Optional.ofNullable(url)
				.flatMap(connection::connectToAndGet);
	}
}

