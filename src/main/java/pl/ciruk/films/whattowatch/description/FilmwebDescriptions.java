package pl.ciruk.films.whattowatch.description;

import static pl.ciruk.core.net.JsoupConnection.connectTo;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import pl.ciruk.films.whattowatch.title.Title;

public class FilmwebDescriptions implements DescriptionProvider {

	private static final String ORIGINAL_TITLE = null;

	@Override
	public Optional<Description> descriptionOf(Title title) {
		return Stream.of(title.getTitle(), title.getOriginalTitle())
				.filter(t -> t != null && !t.isEmpty())
				.parallel()
				.flatMap(t -> filmsForTitle(t, title.getYear()))
				.findAny();
	}

	public Stream<Description> filmsForTitle(String title, int year) {
		Document page = searchFor(title, year);
		return page.select("ul.resultsList li .filmPreview .filmTitle a")
				.stream()
				.parallel()
				.peek(d -> System.out.println(d))
				.map(d -> getPageWithSearchResultsFor(d))
				.map(details -> {
					String localTitle = details.select(".filmTitle .h1").first().text();
					String originalTitle = details.select(ORIGINAL_TITLE).first().text();
					Title titles = new Title(localTitle, originalTitle, year);
					
					Description film = new Description(titles);
					
					return film;
				});
	}

	private Document getPageWithSearchResultsFor(Element d) {
		try {
			return connectTo("http://filmweb.pl/" + d.attr("href")).get();
		} catch (IOException e) {
			throw new RuntimeException(e); 
		}
	}
	
	Document searchFor(String title, int year) {
		String url;
		try {
			url = String.format("http://www.filmweb.pl/search/film?q=%s&startYear=%d&endYear=%d", 
					URLEncoder.encode(title, Charset.defaultCharset().name()), 
					year, 
					year);
			
			return connectTo(url)
					.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

