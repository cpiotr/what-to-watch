package pl.ciruk.films.whattowatch;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;



import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;



import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Path("films")
public class EkinoScrapper {
	static ExecutorService executorService = Executors.newCachedThreadPool();
	
	@GET
	public void listOfFilms() {
		try {
			Document document = getPage(0);
			
			int numberOfPages = Integer.valueOf(document.select("ul.pagination li").last().text());
			IntStream.range(0, 1)
					.parallel()
					.forEach(i -> {
						try {
							process(getPage(i));
						} catch (Exception e) {
							System.err.println("Cannot download page number: " + i);
							System.err.println(e.getMessage());
							e.printStackTrace();
						}
					});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	Document getPage(int pageNumber) throws IOException {
		Document document = Jsoup.connect(String.format("http://www.ekino.tv/kategorie,%d,dubbing-lektor-napisy-polskie,,2008-2014,,.html", pageNumber))
				.timeout(60 * 1000)
				.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0")
				.referrer("http://www.ekino.tv/")
				.data("sort_field", "data-dodania")
				.data("sort_method", "desc")
				.get();
		
		return document;
	}
	
	void process(Document page) {
		Elements elements = page.select("ul.movies li");
		elements.stream()
			.map(li -> {
				String polishTitle = li.select(".title h2 a").first().text();
				String originalTitle = li.select(".title h3 a").first().text();
				
				Film film = new Film();
				film.year = getYear(li);
				
				film.title = polishTitle;
				film.originalTitle = originalTitle;
				return film;
			})
			.parallel()
			.forEach(e -> {
				String url = String.format("http://www.omdbapi.com/?t=%s&y=%d", URLEncoder.encode(e.title), e.year);
				JsonObject json = null;
				try (InputStream inputStream = new URL(url).openStream()) {
					json = Json.createReader(inputStream).readObject();
					if (json.getString("Response").equalsIgnoreCase("true")) {
						System.out.println(json);
					}
				} catch (IOException mue) {
					System.err.println("Cannot get  " + e.title + " " + e.year + " ");
					System.out.println(mue.getMessage());
					//mue.printStackTrace();
				}
				
				if (json.getString("Response").equalsIgnoreCase("false")) {
					url = String.format("http://www.omdbapi.com/?t=%s&y=%d", URLEncoder.encode(e.originalTitle), e.year);
					try (InputStream inputStream = new URL(url).openStream()) {
						json = Json.createReader(inputStream).readObject();
						System.out.println(json);
					} catch (IOException mue) {
						System.err.println("Cannot get  " + e.title + " " + e.year + " ");
						System.out.println(mue.getMessage());
						//mue.printStackTrace();
					}
					
				}
			});
	}

	static class Film {
		String title;
		String originalTitle;
		int year;
	}
	
	private int getYear(Element li) {
		return Integer.valueOf(li
				.select(".title p.a a")
				.stream()
				.filter(a -> a.attr("href").contains("year"))
				.map(a -> a.text())
				.findFirst()
				.get());
	}
}
