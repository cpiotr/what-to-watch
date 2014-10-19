package pl.ciruk.films.whattowatch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import pl.ciruk.films.whattowatch.EkinoScrapper.Film;
import pl.ciruk.films.whattowatch.EkinoScrapper.Score;

public class FilmwebScrapper {
	private static final String RATING = ".filmRateInfo .filmRate strong";
	
	private static final String VOTES = ".filmRateInfo .votesCount strong";
	
	private static final String ORIGINAL_TITLE = ".filmTitle h2";
	
	public Stream<Film> filmsForTitle(String title, int year) {
		Document page = searchFor(title, year);
		return page.select("ul.resultsList li .filmPreview .filmTitle a")
				.stream()
				.parallel()
				.peek(d -> System.out.println(d))
				.map(d -> get("http://filmweb.pl/" + d.attr("href")))
				.map(details -> {
					Film film = new Film();
					film.title = details.select(".filmTitle .h1").first().text();
					film.originalTitle = details.select(ORIGINAL_TITLE).first().text();
					film.year = year;
					
					double score = Double.valueOf(details.select(RATING).first().text().replaceAll(",", ".")) / 10.0;
					int votes = Integer.valueOf(details.select(VOTES).first().text());
					film.scores.add(new Score(score, votes));
					return film;
				});
	}
	
	Document searchFor(String title, int year) {
		String url;
		try {
			url = String.format("http://www.filmweb.pl/search/film?q=%s&startYear=%d&endYear=%d", 
					URLEncoder.encode(title, Charset.defaultCharset().name()), 
					year, 
					year);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return get(url);
	}

	private Document get(String url) {
		try {
			return Jsoup.connect(url)
					.timeout(60 * 1000)
					.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0")
					.referrer("http://www.filmweb.pl/")
					.get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	Document detailsFor(String url) {
		return get(url);
	}
	
	public static void main(String[] args) {
		FilmwebScrapper filmwebScrapper = new FilmwebScrapper();
		filmwebScrapper.filmsForTitle("Szklana pu³apka", 2007)
				.forEach(f -> System.out.println(f.scores));
	}
}
