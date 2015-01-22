package pl.ciruk.films.whattowatch.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.films.whattowatch.net.Extractable;
import pl.ciruk.core.text.NumberToken;
import pl.ciruk.core.text.NumberTokenizer;

import java.util.Optional;
import java.util.function.Function;

public enum MetacriticSelectors implements Extractable<Optional<String>> {
	LINK_TO_DETAILS(details -> details.select(".product_title a")
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
	LINK_TO_CRITIC_REVIEWS(details -> details.select(".critic_reviews_module .module_title a")
			.stream()
			.map(link -> link.attr("href"))
			.findFirst()),
	TITLE(details -> details.select(".product_title")
			.stream()
			.map(Element::text)
			.findFirst()),
	RELEASE_DATE(details -> details.select(".release_date .data")
			.stream()
			.map(Element::text)
			.findFirst()),
	;
	private Function<Element, Optional<String>> extractor;

	private MetacriticSelectors(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Optional<String> extractFrom(Element details) {
		return extractor.apply(details);
	}
}
