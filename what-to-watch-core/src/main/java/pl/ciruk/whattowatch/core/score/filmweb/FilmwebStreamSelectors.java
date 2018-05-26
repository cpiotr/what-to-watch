package pl.ciruk.whattowatch.core.score.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum FilmwebStreamSelectors implements Extractable<Stream<Element>> {
    FILMS_FROM_SEARCH_RESULT(page -> page.select("ul.resultsList li .filmPreview__card .filmPreview__header")
            .stream()),;
    private Function<Element, Stream<Element>> extractor;

    FilmwebStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
