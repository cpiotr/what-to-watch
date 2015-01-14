package pl.ciruk.films.whattowatch.title.ekino;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;

public class EkinoTitles implements TitleProvider {
	private static final String ROOT_URL = "http://www.ekino.tv/";
	private JsoupConnection connection;

	Map<String, String> urls;

	public EkinoTitles(JsoupConnection connection) {
		this.connection = connection;
		this.urls = new HashMap<>();
	}



	@Override
	public Stream<Title> streamOfTitles(int numberOfTitles) {
		try {
			Document document = getPage(0);

			int numberOfPages = Integer.valueOf(
					EkinoSelectors.NUMBER_OF_PAGES.extractFrom(document).orElse("10"));
			
			return IntStream.range(0, 10)
					.parallel()
					.mapToObj(i -> i)
					.flatMap(i -> {
						try {
							return process(getPage(i));
						} catch (Exception e) {
							//System.err.println("Cannot download page number: " + i);
							throw new RuntimeException(e);
						}
					}); 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String urlFor(Title title) {
		String key = computeKeyFrom(title);
		return ROOT_URL + urls.getOrDefault(key, "");
	}

	Document getPage(int pageNumber) throws IOException {
		Document document = connection.to(String.format(ROOT_URL + "kategorie,%d,dubbing-lektor-napisy-polskie,,2008-2014,,.html", pageNumber))
				.data("sort_field", "data-dodania")
				.data("sort_method", "desc")
				.get();
		
		return document;
	}
	
	Stream<Title> process(Document page) {
		Elements elements = page.select("ul.movies li");
		return elements.stream()
			.map(li -> {
				String polishTitle = EkinoSelectors.LOCAL_TITLE.extractFrom(li).orElse("");
				String originalTitle = EkinoSelectors.ORIGINAL_TITLE.extractFrom(li).orElse("");
				int year = Integer.valueOf(
						EkinoSelectors.YEAR.extractFrom(li).orElse("0"));
				Title title = Title.builder()
						.title(polishTitle)
						.originalTitle(originalTitle)
						.year(year)
						.build();

				urls.putIfAbsent(computeKeyFrom(title), EkinoSelectors.LINK_TO_FILM.extractFrom(li).orElse(""));

				return title;

			});
	}

	private String computeKeyFrom(Title title) {
		return Strings.isNullOrEmpty(title.getOriginalTitle())
				? title.getTitle()
				: title.getOriginalTitle();
	}
}
