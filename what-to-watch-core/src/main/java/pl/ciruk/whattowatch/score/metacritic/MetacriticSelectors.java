package pl.ciruk.whattowatch.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;
import pl.ciruk.core.text.NumberToken;
import pl.ciruk.core.text.NumberTokenizer;

import java.util.Optional;
import java.util.function.Function;

public enum MetacriticSelectors implements Extractable<Optional<String>> {
    LINK_TO_DETAILS(details -> details.select(".product_title a")
            .stream()
            .map(link -> link.attr("href"))
            .findFirst()),
    AVERAGE_GRADE(details -> details.select(".simple_summary .metascore_w.movie")
            .stream()
            .map(Element::text)
            .findFirst()
            .filter(MetacriticSelectors::isValidScore)),
    NUMBER_OF_GRADES(details -> Optional.of(String.valueOf(
            details.select(".simple_summary div.chart div.count")
                    .stream()
                    .map(Element::text)
                    .map(NumberTokenizer::new)
                    .filter(NumberTokenizer::hasMoreTokens)
                    .map(NumberTokenizer::nextToken)
                    .mapToLong(NumberToken::asSimpleLong)
                    .sum()))),
    NEW_YORK_TIMES_GRADE(details -> details.select(".critic_reviews .review")
            .stream()
            .filter(review -> review.select(".source").text().equalsIgnoreCase("The New York Times"))
            .map(review -> review.select(".metascore_w.movie").text())
            .findFirst()),
    LINK_TO_CRITIC_REVIEWS(details -> details.select(".fxdcol.gu4 .subsection_title a")
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
            .map(MetacriticSelectors::extractYear)
            .findFirst()),;

    private static boolean isValidScore(String scoreAsText) {
        return !scoreAsText.isEmpty() && !scoreAsText.equalsIgnoreCase("tbd");
    }

    private static String extractYear(String dateAsText) {
        return dateAsText.replaceAll(".+, ", "");
    }

    private Function<Element, Optional<String>> extractor;

    MetacriticSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }
}
