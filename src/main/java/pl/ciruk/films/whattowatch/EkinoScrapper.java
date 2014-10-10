package pl.ciruk.films.whattowatch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Path("films")
public class EkinoScrapper {
	static ExecutorService executorService = Executors.newCachedThreadPool();
	
	MetacriticScrapper metacriticScrapper = new MetacriticScrapper();
	
	@GET
	@Produces("application/json")
	public Response listOfFilms() {
		List<Film> films = new ArrayList<>();
		try {
			Document document = getPage(0);
			
			int numberOfPages = Integer.valueOf(document.select("ul.pagination li").last().text());
			
			films = IntStream.range(0, 10)
					.parallel()
					.mapToObj(i -> {
						try {
							return process(getPage(i));
						} catch (Exception e) {
							//System.err.println("Cannot download page number: " + i);
							throw new RuntimeException(e);
						}
					})
					.reduce(new ArrayList<Film>(), (result, partial) -> {
						result.addAll(partial);
						return new ArrayList<>(result);
					}); 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return Response.ok(films).build();
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
	
	List<Film> process(Document page) {
		Elements elements = page.select("ul.movies li");
		return elements.stream()
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
			.peek(e -> {
				String url = String.format("http://www.omdbapi.com/?t=%s&y=%d", URLEncoder.encode(e.title), e.year);
				JsonObject json = null;
				try (InputStream inputStream = new URL(url).openStream()) {
					json = Json.createReader(inputStream).readObject();
					if (json.getString("Response").equalsIgnoreCase("true")) {
						//System.out.println(json);
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
						//System.out.println(e.originalTitle +  " " + json);
					} catch (IOException mue) {
						System.err.println("Cannot get  " + e.title + " " + e.year + " ");
						System.out.println(mue.getMessage());
						//mue.printStackTrace();
					}
					
				}
				if (json != null) {
					if (json.containsKey("Title") && json.containsKey("imdbRating")) {
						e.imdbTitle = json.getString("Title");
						String rating = json.getString("imdbRating");
						if (rating.matches("-?[0-9]+[.]?[0-9]+")) {
							Score score = new Score(Double.valueOf(rating) / 10.0, Integer.valueOf(json.getString("imdbVotes").replaceAll("[^0-9]", "")));
							e.scores.add(score);
							try {
								e.scores.add(metacriticScrapper.scoreFor(e));
							} catch (Exception e1) {
								System.err.println(e1.getMessage());
							}
						}
					} else {
						System.out.println("Brak: " + e.title);
					}
				}
			})
			.filter(f -> f.scores.size() == 2)
			.collect(Collectors.toList());
	}

	@XmlRootElement
	static class Film {
		@XmlElement
		String title;
		@XmlElement
		String originalTitle;
		@XmlElement
		String imdbTitle;
		String imdbId;
		@XmlElement
		int year;
		
		@XmlElement
		List<Score> scores = new ArrayList<>();
	}
	
	@XmlRootElement
	static class Score {
		
		public Score() {
			// TODO Auto-generated constructor stub
		}
		
		public Score(double score, int quantity) {
			this.value = score;
			this.quantity = quantity;
		}
		@XmlElement
		double value;
		@XmlElement
		long quantity;
		
		@Override
		public String toString() {
			return String.format("[%f, %d]", value, quantity);
		}
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
