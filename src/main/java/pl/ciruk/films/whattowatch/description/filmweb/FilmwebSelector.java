package pl.ciruk.films.whattowatch.description.filmweb;

import static pl.ciruk.core.net.JsoupNodes.emptyTextElement;

import java.util.Optional;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;
import pl.ciruk.core.net.JsoupNodes;

public enum FilmwebSelector implements Extractable {
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
	;
	private Function<Element, Optional<String>> extractor;
	
	private FilmwebSelector(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	public Optional<String> extractFrom(Element details) {
		return extractor.apply(details);
	}
}
