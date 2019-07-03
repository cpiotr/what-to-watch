package pl.ciruk.whattowatch.core.title.onetwothree;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum OneTwoThreeSelectors implements Extractable<Optional<String>> {
    TITLE(film -> film.select(".data h1")
            .stream()
            .findFirst()
            .map(Element::text)
            .map(OneTwoThreeSelectors::stripTags)),
    ORIGINAL_TITLE(film -> film.select(".data h1")
            .stream()
            .findFirst()
            .map(Element::text)),
    HREF(film -> film.select(".titlecover")
            .stream()
            .findFirst()
            .map(e -> e.attr("href"))),
    YEAR(film -> film.select(".data .custom_fields .date")
            .stream()
            .findFirst()
            .map(Element::text)
            .map(OneTwoThreeSelectors::trimToYear)),
    ;

    private final Function<Element, Optional<String>> extractor;

    OneTwoThreeSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element element) {
        return extractor.apply(element);
    }

    private static String trimToYear(String wholeDescriptionInTitle) {
        int endOfFirstPart = wholeDescriptionInTitle.lastIndexOf(' ');
        return wholeDescriptionInTitle.substring(endOfFirstPart).trim();
    }

    private static String stripTags(String title) {
        int indexOfDash = title.indexOf('-');
        if (indexOfDash > -1) {
            return title.substring(0, indexOfDash);
        }

        return title;
    }

}
