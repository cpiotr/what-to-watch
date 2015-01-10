package pl.ciruk.films.whattowatch.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;
import pl.ciruk.core.net.JsoupNodes;
import pl.ciruk.core.text.MissingValueException;
import pl.ciruk.core.text.NumberToken;
import pl.ciruk.core.text.NumberTokenizer;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public enum MetacriticSelector implements Extractable {
	LINK_TO_DETAILS(details -> details.select("product_title a")
			.stream()
			.map(link -> link.attr("href"))
			.findFirst()),
	AVERAGE_GRADE(details -> details.select(".metascore_summary .metascore_w.movie span")
			.stream()
			.map(Element::text)
			.findFirst()),
	NUMBER_OF_GRADES(details -> details.select(".metascore_summary .summary .count strong")
			.stream()
			.map(Element::text)
			.map(NumberTokenizer::new)
			.filter(NumberTokenizer::hasMoreTokens)
			.map(NumberTokenizer::nextToken)
			.map(NumberToken::asString)
			.findFirst()),
	NEW_YORK_TIMES_GRADE(details -> details.select(".reviews .review")
			.stream()
			.filter(review -> review.select(".source").text().equalsIgnoreCase("The New York Times"))
			.map(review -> review.select(".review_grade").text())
			.findFirst()),
	;
	private Function<Element, Optional<String>> extractor;

	private MetacriticSelector(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Optional<String> extractFrom(Element details) {
		return extractor.apply(details);
	}
}
