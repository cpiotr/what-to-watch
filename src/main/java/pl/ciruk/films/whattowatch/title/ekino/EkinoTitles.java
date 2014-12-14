package pl.ciruk.films.whattowatch.title.ekino;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.Film;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;

public class EkinoTitles implements TitleProvider {
	private JsoupConnection connection;


	public EkinoTitles(JsoupConnection connection) {
		this.connection = connection;
	}
	
	@Override
	public Stream<Title> streamOfTitles(int numberOfTitles) {
		try {
			Document document = getPage(0);
			
			int numberOfPages = Integer.valueOf(document.select("ul.pagination li").last().text());
			
			return IntStream.range(0, 2)
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

	Document getPage(int pageNumber) throws IOException {
		Document document = connection.to(String.format("http://www.ekino.tv/kategorie,%d,dubbing-lektor-napisy-polskie,,2008-2014,,.html", pageNumber))
				.data("sort_field", "data-dodania")
				.data("sort_method", "desc")
				.get();
		
		return document;
	}
	
	Stream<Title> process(Document page) {
		Elements elements = page.select("ul.movies li");
		return elements.stream()
			.map(li -> {
				String polishTitle = EkinoSelector.LOCAL_TITLE.extractFrom(li);
				String originalTitle = EkinoSelector.ORIGINAL_TITLE.extractFrom(li);
				int year = Integer.valueOf(
						EkinoSelector.YEAR.extractFrom(li));
				return new Title(polishTitle, originalTitle, year);
			});
	}
}
