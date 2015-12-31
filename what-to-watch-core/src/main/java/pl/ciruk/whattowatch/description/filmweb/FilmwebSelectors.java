package pl.ciruk.whattowatch.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum FilmwebSelectors implements Extractable<Optional<String>> {
	YEAR(details -> details.select(".filmMainHeader .hdr span.halfSize")
			.stream()
			.map(Element::text)
			.map(text -> text.replaceAll("[^0-9]", ""))
			.findFirst()),
	LOCAL_TITLE(details -> details.select(".filmMainHeader h1.filmTitle")
			.stream()
			.map(Element::text)
			.findFirst()),
	ORIGINAL_TITLE(details -> details.select(".filmMainHeader h2")
			.stream()
			.map(Element::text)
			.findFirst()),
	POSTER(details -> details.select(".filmHeader .filmPosterBox .posterLightbox img")
			.stream()
			.map(e -> e.attr("src"))
			.findFirst()),
	PLOT(details -> details.select(".filmMainHeader .filmPlot")
			.stream()
			.map(Element::text)
			.findFirst()),
	;

	public static final String ROOT_URL = "http://filmweb.pl";

	private Function<Element, Optional<String>> extractor;
	
	private FilmwebSelectors(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Optional<String> extractFrom(Element details) {
		return extractor.apply(details);
	}
}
