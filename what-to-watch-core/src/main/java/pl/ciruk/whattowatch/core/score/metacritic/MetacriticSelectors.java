package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.Extractable;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public enum MetacriticSelectors implements Extractable<Optional<String>> {
    LINK_TO_DETAILS(details -> details.select(".product_title a")
            .stream()
            .map(link -> link.attr("href"))
            .findFirst()),
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
    RELEASE_YEAR(details -> details.select(".main_stats p")
            .stream()
            .map(Element::text)
            .map(MetacriticSelectors::extractYear)
            .filter(MetacriticSelectors::isValidNumber)
            .findFirst()),
    ;

    private static final Pattern NUMBER = Pattern.compile("[0-9]+");

    private static boolean isValidNumber(String yearAsText) {
        return NUMBER.matcher(yearAsText).matches();
    }

    private static String extractYear(String dateAsText) {
        return dateAsText.replaceAll(".+, ", "");
    }

    private final Function<Element, Optional<String>> extractor;

    MetacriticSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }
}
