package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.Extractable;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public enum MetacriticSelectors implements Extractable<Optional<String>> {
    LINK_TO_DETAILS(details -> Optional.ofNullable(details.selectFirst("h3.product_title a"))
            .map(link -> link.attr("href"))),
    NEW_YORK_TIMES_GRADE(details -> details.select("div.critic_reviews div.review")
            .stream()
            .filter(review -> review.select("span.source").text().equalsIgnoreCase("The New York Times"))
            .map(review -> review.select("div.metascore_w.movie").text())
            .findFirst()),
    LINK_TO_CRITIC_REVIEWS(details -> Optional.ofNullable(details.selectFirst("div.fxdcol.gu4 .subsection_title a"))
            .map(link -> link.attr("href"))),
    TITLE(details -> Optional.ofNullable(details.selectFirst("h3.product_title"))
            .map(Element::text)),
    RELEASE_YEAR(details -> Optional.ofNullable(details.selectFirst("div.main_stats p"))
            .map(Element::text)
            .map(MetacriticSelectors::extractYear)
            .filter(MetacriticSelectors::isValidNumber)),
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
