package pl.ciruk.films.whattowatch.description.filmweb;

import java.util.function.Function;

import org.jsoup.nodes.Document;

public enum FilmwebSelector {
	YEAR(details -> details.select(".filmMainHeader .hdr span.halfSize")
					.first()
					.text()
					.replaceAll("[^0-9]", "")),
	LOCAL_TITLE(details -> details.select(".filmMainHeader h1.filmTitle")
			.first()
			.text()),
	ORIGINAL_TITLE(details -> details.select(".filmMainHeader h2")
			.first()
			.text()),
	;
	private Function<Document, String> extractor;;
	
	private FilmwebSelector(Function<Document, String> extractor) {
		this.extractor = extractor;
	}

	public String extractFrom(Document details) {
		return extractor.apply(details);
	}
}
