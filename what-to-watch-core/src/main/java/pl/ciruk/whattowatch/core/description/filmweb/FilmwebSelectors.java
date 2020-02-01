package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;
import pl.ciruk.whattowatch.utils.text.Patterns;

import java.util.Optional;
import java.util.function.Function;

public enum FilmwebSelectors implements Extractable<Optional<String>> {
    YEAR(details -> {
        var yearElement = details.selectFirst("span.filmCoverSection__year");
        return Optional.ofNullable(yearElement)
                .map(Element::text)
                .map(text -> Patterns.nonDigit().matcher(text).replaceAll(""));
    }),
    LOCAL_TITLE(details -> Optional.ofNullable(details.selectFirst("h1.filmCoverSection__title a"))
            .map(Element::text)),
    ORIGINAL_TITLE(details -> Optional.ofNullable(details.selectFirst("h2.filmCoverSection__orginalTitle"))
            .map(Element::text)),
    POSTER(details -> Optional.ofNullable(details.selectFirst("div.filmPosterSection__container div.filmPosterSection__poster img"))
            .map(e -> e.attr("src"))),
    PLOT(details -> Optional.ofNullable(details.selectFirst("div.filmPosterSection__container div.filmPosterSection__plot"))
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
