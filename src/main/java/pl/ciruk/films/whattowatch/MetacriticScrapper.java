package pl.ciruk.films.whattowatch;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import pl.ciruk.films.whattowatch.EkinoScrapper.Film;
import pl.ciruk.films.whattowatch.EkinoScrapper.Score;

public class MetacriticScrapper {
	public Score scoreFor(Film film) throws IOException {
		return searchFor(film.imdbTitle)
				.select("ul.search_results li.result .product_title a")
				.stream()
				.filter(a -> a.text().equalsIgnoreCase(film.imdbTitle))
				.findFirst()
				.flatMap(a -> {
					Document details = null;
					try {
						details = detailsFor(a.attr("href"));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Stream<Element> elements = details
							.select(".product_scores .metascore_summary span")
							.stream();
					
					
					double score = elements
							.filter(e -> e.hasAttr("itemprop") && e.attr("itemprop").equals("ratingValue"))
							.mapToDouble(e -> Double.valueOf(e.text()))
							.map(d -> d / 100.0)
							.sum();
					elements = details
							.select(".product_scores .metascore_summary span")
							.stream();
					int quantity = elements
							.filter(e -> e.hasAttr("itemprop") && e.attr("itemprop").equals("reviewCount"))
							.mapToInt(e -> Integer.valueOf(e.text()))
							.sum();
					
					return Optional.of(new Score(score, quantity));
				}).get();
	}
	

	public void finalize() throws Throwable {
		super.finalize();
		System.out.println("I'm done");
	}
	
	Document searchFor(String title) throws IOException {
		Document document = Jsoup.connect(String.format("http://www.metacritic.com/search/all/%s/results", title))
				.timeout(60 * 1000)
				.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0")
				.referrer("http://www.metacritic.com/")
				.get();
		
		return document;
	}
	
	Document detailsFor(String href) throws IOException {
		Document document = Jsoup.connect(String.format("http://www.metacritic.com/%s", href))
				.timeout(60 * 1000)
				.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0")
				.referrer("http://www.metacritic.com/")
				.get();
		
		return document;
	}
}
