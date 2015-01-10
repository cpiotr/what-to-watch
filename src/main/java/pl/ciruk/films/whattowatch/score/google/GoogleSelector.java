package pl.ciruk.films.whattowatch.score.google;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

import static pl.ciruk.core.net.JsoupNodes.emptyTextElement;

public enum GoogleSelector implements Extractable {
	SCORE(details -> details.select("ol#rso li.g div.slp")
			.stream()
			.map(Element::text)
			.filter(s -> !s.isEmpty())
			.findFirst())
	;
	private Function<Element, Optional<String>> extractor;

	private GoogleSelector(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	public Optional<String> extractFrom(Element details) {
		return extractor.apply(details);
	}
}
