package pl.ciruk.films.whattowatch.title.ekino;

import com.google.common.base.Strings;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.ciruk.films.whattowatch.net.JsoupConnection;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Named
public class EkinoTitles implements TitleProvider {
	private static final String ROOT_URL = "http://www.ekino.tv/";

	private JsoupConnection connection;

	Map<String, String> urls;

	@Inject
	public EkinoTitles(JsoupConnection connection) {
		this.connection = connection;
		this.urls = new HashMap<>();
	}

	@Override
	public Stream<Title> streamOfTitles() {
		int numberOfPages = getFilmsPageOfNumber(0)
				.flatMap(EkinoSelectors.NUMBER_OF_PAGES::extractFrom)
                .map(Integer::valueOf)
                .orElse(1);
		//TODO: Logger
		System.out.println("Number of pages: " + numberOfPages);

		return IntStream.range(0, numberOfPages)
                .parallel()
                .mapToObj(i -> i)
                .flatMap(i -> getFilmsPageOfNumber(i)
						.map(page -> retrieveTitlesFrom(page))
						.orElse(Stream.empty()));
	}

	@Override
	public String urlFor(Title title) {
		String key = computeKeyFrom(title);
		return ROOT_URL + urls.getOrDefault(key, "");
	}

	Optional<Document> getFilmsPageOfNumber(int pageNumber) {
		Document document = null;
		try {
			document = connection.to(String.format(ROOT_URL + "kategorie,%d,dubbing-lektor-napisy-polskie,,2008-2014,,.html", pageNumber))
                    .data("sort_field", "data-dodania")
                    .data("sort_method", "desc")
                    .get();
		} catch (IOException e) {
			// TODO: Logger
			e.printStackTrace();
		}

		return Optional.ofNullable(document);
	}
	
	Stream<Title> retrieveTitlesFrom(Document filmListPage) {
		return EkinoStreamSelectors.FILMS.extractFrom(filmListPage)
				.map(this::titleFrom);
	}

	Title titleFrom(Element filmAsHtml) {
		String polishTitle = EkinoSelectors.LOCAL_TITLE.extractFrom(filmAsHtml).orElse("");
		String originalTitle = EkinoSelectors.ORIGINAL_TITLE.extractFrom(filmAsHtml).orElse("");
		int year = Integer.valueOf(
                EkinoSelectors.YEAR.extractFrom(filmAsHtml).orElse("0"));
		Title title = Title.builder()
                .title(polishTitle)
                .originalTitle(originalTitle)
                .year(year)
                .build();

		urls.putIfAbsent(computeKeyFrom(title), EkinoSelectors.LINK_TO_FILM.extractFrom(filmAsHtml).orElse(""));

		return title;
	}

	private String computeKeyFrom(Title title) {
		return Strings.isNullOrEmpty(title.getOriginalTitle())
				? title.getTitle()
				: title.getOriginalTitle();
	}
}
