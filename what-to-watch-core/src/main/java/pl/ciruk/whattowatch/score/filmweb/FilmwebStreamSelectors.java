package pl.ciruk.whattowatch.score.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum FilmwebStreamSelectors implements Extractable<Stream<Element>> {
    FILMS_FROM_SEARCH_RESULT(page -> page.select("ul.resultsList li .hitDesc .hitDescWrapper")
            .stream()),
    ;
    private Function<Element, Stream<Element>> extractor;

    FilmwebStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}