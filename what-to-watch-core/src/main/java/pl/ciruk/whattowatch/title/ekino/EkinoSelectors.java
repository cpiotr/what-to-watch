package pl.ciruk.whattowatch.title.ekino;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum EkinoSelectors implements Extractable<Optional<String>> {
    TITLE(film -> film.select(".title a")
            .stream()
            .findFirst()
            .map(Element::text)),
    ORIGINAL_TITLE(film -> film.select(".title .blue a")
            .stream()
            .findFirst()
            .map(Element::text)),
    HREF(film -> film.select(".title a")
            .stream()
            .findFirst()
            .map(e -> e.attr("href"))),
    YEAR(film -> film.select(".info-categories .cates")
            .stream()
            .findFirst()
            .map(Element::text)
            .map(EkinoSelectors::trimToYear));
    private Function<Element, Optional<String>> extractor;

    EkinoSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element element) {
        return extractor.apply(element);
    }

    private static String trimToYear(String wholeDescriptionInTitle) {
        int endOfFirstPart = wholeDescriptionInTitle.indexOf('|');
        return wholeDescriptionInTitle.substring(0, endOfFirstPart).trim();
    }

}
