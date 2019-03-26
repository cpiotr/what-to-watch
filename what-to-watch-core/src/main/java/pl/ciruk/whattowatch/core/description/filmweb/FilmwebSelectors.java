package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum FilmwebSelectors implements Extractable<Optional<String>> {
    YEAR(details -> {
        var yearElement = details.selectFirst("div.hdr span.halfSize");
        return Optional.ofNullable(yearElement)
                .map(Element::text)
                .map(text -> Commons.NON_DIGIT.matcher(text).replaceAll(""));
    }),
    LOCAL_TITLE(details -> Optional.ofNullable(details.selectFirst("h1.filmTitle a"))
            .map(Element::text)),
    ORIGINAL_TITLE(details -> Optional.ofNullable(details.selectFirst("h2"))
            .map(Element::text)),
    POSTER(details -> Optional.ofNullable(details.selectFirst("div.filmHeader div.filmPosterBox div.posterLightbox img"))
            .map(e -> e.attr("src"))),
    PLOT(details -> Optional.ofNullable(details.selectFirst("div.filmPlot"))
            .map(Element::text)),
    ;

    private final Function<Element, Optional<String>> extractor;

    FilmwebSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }
}
