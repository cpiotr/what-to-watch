package pl.ciruk.whattowatch.core.score.imdb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum ImdbStreamSelectors implements Extractable<Stream<Element>> {
    FILMS_FROM_SEARCH_RESULT(page -> page.select(".lister-list .lister-item")
            .stream()),
    ;
    private final Function<Element, Stream<Element>> extractor;

    ImdbStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
