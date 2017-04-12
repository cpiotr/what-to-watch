package pl.ciruk.whattowatch.score.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum FilmwebSelectors implements Extractable<Optional<String>> {
    YEAR(details -> details.select("span.hitTitle")
            .stream()
            .map(Element::text)
            .map(text -> text.replaceAll("[^0-9]", ""))
            .findFirst()),
    LOCAL_TITLE(details -> details.select("a.hitTitle")
            .stream()
            .map(Element::text)
            .findFirst()),
    SCORE(details -> details.select(".rateInfo .box")
            .stream()
            .map(Element::text)
            .findFirst()),;

    private Function<Element, Optional<String>> extractor;

    FilmwebSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }
}
