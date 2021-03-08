package pl.ciruk.whattowatch.core.title.onetwothree;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum OneTwoThreeSelectors implements Extractable<Optional<String>> {
    TITLE(film -> film.select("h3 a")
            .stream()
            .findFirst()
            .map(Element::text)),
    ORIGINAL_TITLE(film -> film.select("h3 a")
            .stream()
            .findFirst()
            .map(Element::text)),
    HREF(film -> film.select("a")
            .stream()
            .findFirst()
            .map(e -> e.attr("href"))),
    YEAR(film -> film.select(".meta")
            .stream()
            .findFirst()
            .map(Element::text)
            .flatMap(OneTwoThreeSelectors::extractYear)),
    ;

    private final Function<Element, Optional<String>> extractor;

    OneTwoThreeSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element element) {
        return extractor.apply(element);
    }

    private static Optional<String> extractYear(String meta) {
        boolean isYear = true;
        for (int i = 0; i < 4; i++) {
            if (i >= meta.length() || !Character.isDigit(meta.charAt(i))) {
                isYear = false;
            }
        }
        return isYear
                ? Optional.of(meta.substring(0, 4))
                : Optional.empty();
    }
}
