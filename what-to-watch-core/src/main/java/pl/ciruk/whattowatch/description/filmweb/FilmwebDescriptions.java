package pl.ciruk.whattowatch.description.filmweb;

import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.core.text.MissingValueException;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.title.Title;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

@Named
@Slf4j
public class FilmwebDescriptions implements DescriptionProvider {

	private final JsoupConnection connection;

	private final ExecutorService executorService;

	@Inject
	public FilmwebDescriptions(@Named("allCookies") JsoupConnection connection, ExecutorService executorService) {
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
				.map(page -> page.select("ul.resultsList li .hitDesc .hitDescWrapper h3 a")
								.stream()
								.map(this::getPageWithFilmDetailsFor)
								.map(pageWithDetails -> {
									try {
										return extractDescriptionFrom(pageWithDetails);
									} catch (MissingValueException e) {
										log.warn("filmsForTitle - Could not get description for {} ({})", title, year);
										return null;
									}
								}).filter(Objects::nonNull))
				.orElse(Stream.empty());
	}

	private Description extractDescriptionFrom(Element pageWithDetails) {
		String localTitle = FilmwebSelectors.LOCAL_TITLE.extractFrom(pageWithDetails)
				.orElse("");
		String originalTitle = FilmwebSelectors.ORIGINAL_TITLE.extractFrom(pageWithDetails)
				.orElse("");
		int extractedYear = extractYearFrom(pageWithDetails);

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

	private Integer extractYearFrom(Element details) {
		return FilmwebSelectors.YEAR.extractFrom(details)
                .map(this::parseYear)
                .orElseThrow(MissingValueException::new);
	}

	private Integer parseYear(String extractFrom) {
		return Integer.valueOf(extractFrom);
	}

	private Element getPageWithFilmDetailsFor(Element d) {
		return connection.connectToAndGet(FilmwebSelectors.ROOT_URL + d.attr("href")).get();
	}

	Optional<Element> searchFor(String title, int year) {
		String url;
		try {
			url = String.format("%s/search/film?q=%s&startYear=%d&endYear=%d",
					FilmwebSelectors.ROOT_URL,
					URLEncoder.encode(title, Charset.defaultCharset().name()),
					year,
					year);

			return connection.connectToAndGet(url);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		FilmwebDescriptions descriptions=new FilmwebDescriptions(new JsoupCachedConnection(CacheProvider.empty(), new OkHttpClient()), Executors.newFixedThreadPool(8));
		Description paramObject = descriptions.descriptionOf(Title.builder().title("Rambo").year(1988).build()).get();
		System.out.println(paramObject);
	}
}

