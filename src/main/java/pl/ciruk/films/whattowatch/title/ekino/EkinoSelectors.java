package pl.ciruk.films.whattowatch.title.ekino;

import java.util.Optional;
import java.util.function.Function;

import org.jsoup.nodes.Element;
import pl.ciruk.films.whattowatch.net.Extractable;

public enum EkinoSelectors implements Extractable<Optional<String>> {
	YEAR(details -> details.select(".title p.a a")
			.stream()
			.filter(a -> a.attr("href").contains("year"))
			.map(Element::text)
			.map(t -> t.replaceAll("[^0-9]", ""))
			.findFirst()),
	LOCAL_TITLE(details -> details.select(".title h2 a")
			.stream()
			.map(Element::text)
			.findFirst()),
	ORIGINAL_TITLE(details -> details.select(".title h3 a")
			.stream()
			.map(Element::text)
			.findFirst()),
	NUMBER_OF_PAGES(results -> {
		Element last = results.select("ul.pagination li").last();
		return Optional.ofNullable(last)
				.map(Element::text);
	}),
	LINK_TO_FILM(details -> details.select(".title h2 a")
			.stream()
			.map(e -> e.attr("href"))
			.findFirst());
	private Function<Element, Optional<String>> extractor;
	
	private EkinoSelectors(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	public Optional<String> extractFrom(Element element) {
		return extractor.apply(element);
	}
}
