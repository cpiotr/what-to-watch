package pl.ciruk.whattowatch.score.imdb;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum ImdbSelectors implements Extractable<Optional<String>> {
	YEAR(details -> details.select(".lister-item-content .lister-item-header .lister-item-year")
			.stream()
			.map(Element::text)
			.map(text -> text.replaceAll("[^0-9]", ""))
			.findFirst()),
	TITLE(details -> details.select(".lister-item-content .lister-item-header a")
			.stream()
			.map(Element::text)
			.findFirst()),
	SCORE(details -> details.select(".ratings-bar .ratings-imdb-rating")
			.stream()
            .filter(div -> div.hasAttr("data-value"))
            .map(div -> div.attr("data-value"))
			.findFirst()),
	NUMBER_OF_SCORES(details -> details.select(".sort-num_votes-visible span")
			.stream()
            .filter(span -> span.hasAttr("data-value"))
            .map(span -> span.attr("data-value"))
			.findFirst()),
	;

	private Function<Element, Optional<String>> extractor;

	ImdbSelectors(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Optional<String> extractFrom(Element details) {
		return extractor.apply(details);
	}
}
