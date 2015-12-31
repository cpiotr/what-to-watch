package pl.ciruk.whattowatch.score.google;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum GoogleSelectors implements Extractable<Optional<String>> {
	SCORE(details -> details.select("ol#rso li.g div.slp")
			.stream()
			.findFirst()
			.filter(e -> !e.select("g-review-stars").isEmpty())
			.map(Element::text)
			.filter(s -> !s.isEmpty()))
	;
	private Function<Element, Optional<String>> extractor;

	private GoogleSelectors(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Optional<String> extractFrom(Element details) {
		return extractor.apply(details);
	}
}
