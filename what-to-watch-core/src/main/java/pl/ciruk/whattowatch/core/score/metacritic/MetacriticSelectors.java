package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public enum MetacriticSelectors implements Extractable<Optional<String>> {
    LINK_TO_DETAILS(details -> Optional.ofNullable(details.selectFirst("div.c-pageSiteSearch-results-item a"))
            .map(link -> link.attr("href"))),
    NEW_YORK_TIMES_GRADE(htmlWithScores -> htmlWithScores.select("p")
            .stream()
            .filter(review -> "The New York Times".equalsIgnoreCase(review.selectFirst("span").text()))
            .map(review -> review.select("div").text())
            .filter(Predicate.not(String::isBlank))
            .findFirst()),
    LINK_TO_FILM_PAGE(details -> Optional.ofNullable(details.selectFirst("a.c-productSubpageHeader_back"))
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
