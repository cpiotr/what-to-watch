package pl.ciruk.whattowatch.core.title.ekino;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public enum EkinoSelectors implements Extractable<Optional<String>> {
    TITLE(film -> film.select(".title a")
            .stream()
            .findFirst()
            .map(Element::text)
            .map(EkinoSelectors::stripTags)),
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
            .flatMap(EkinoSelectors::findYear));

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b\\d{4}\\b");

    private final Function<Element, Optional<String>> extractor;

    EkinoSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element element) {
        return extractor.apply(element);
    }

    // The year leads a description such as "2015 | Komedia, Dramat", which can also come without it
    private static Optional<String> findYear(String description) {
        var matcher = YEAR_PATTERN.matcher(description);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    private static String stripTags(String title) {
        int indexOfDash = title.indexOf('-');
        if (indexOfDash > -1) {
            return title.substring(0, indexOfDash);
        }

        return title;
    }

}
