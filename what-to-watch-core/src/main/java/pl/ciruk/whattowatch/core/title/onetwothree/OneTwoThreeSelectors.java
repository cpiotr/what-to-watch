package pl.ciruk.whattowatch.core.title.onetwothree;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public enum OneTwoThreeSelectors implements Extractable<Optional<String>> {
    TITLE(film -> film.select(".mli-info h2")
            .stream()
            .findFirst()
            .map(Element::text)),
    ORIGINAL_TITLE(film -> film.select(".mli-info h2")
            .stream()
            .findFirst()
            .map(Element::text)),
    HREF(film -> film.select(".ml-mask")
            .stream()
            .findFirst()
            .map(e -> e.attr("href"))),
    YEAR(film -> film.select(".ml-mask")
            .stream()
            .findFirst()
            .map(element -> element.attr("data-url"))
            .flatMap(OneTwoThreeSelectors::trimToYear)),
    ;

    private final Function<Element, Optional<String>> extractor;

    OneTwoThreeSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element element) {
        return extractor.apply(element);
    }

    private static Optional<String> trimToYear(String dataUrl) {
        var urlElements = dataUrl.split("[?&]");
        var prefix = "y=";
        return Stream.of(urlElements)
                .filter(element -> element.startsWith(prefix))
                .map(element -> element.substring(prefix.length()))
                .findFirst();
    }
}
