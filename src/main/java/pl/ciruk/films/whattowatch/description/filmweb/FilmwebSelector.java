package pl.ciruk.films.whattowatch.description.filmweb;

import static pl.ciruk.core.net.JsoupNodes.emptyTextElement;

import java.util.function.Function;

import org.jsoup.nodes.Document;

import pl.ciruk.core.net.JsoupNodes;

public enum FilmwebSelector {
	YEAR(details -> details.select(".filmMainHeader .hdr span.halfSize")
			.stream()
			.findFirst()
			.orElse(emptyTextElement())
			.text()
			.replaceAll("[^0-9]", "")),
	LOCAL_TITLE(details -> details.select(".filmMainHeader h1.filmTitle")
			.stream()
			.findFirst()
			.orElse(emptyTextElement())
			.text()),
	ORIGINAL_TITLE(details -> details.select(".filmMainHeader h2")
			.stream()
			.findFirst()
			.orElse(emptyTextElement())
			.text()),
	;
	private Function<Document, String> extractor;
	
	private FilmwebSelector(Function<Document, String> extractor) {
		this.extractor = extractor;
	}

	public String extractFrom(Document details) {
		return extractor.apply(details);
	}
}
