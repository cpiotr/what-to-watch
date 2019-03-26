package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum FilmwebStreamSelectors implements Extractable<Stream<String>> {
    GENRES(details -> details.select("div.filmInfo ul.genresList li")
            .stream()
            .map(Element::text)),
    LINKS_FROM_SEARCH_RESULT(page -> page.select("ul.resultsList li div.filmPreview__card div.filmPreview__header a")
            .stream()
            .map(a -> a.attr("href"))),;
    private final Function<Element, Stream<String>> extractor;

    FilmwebStreamSelectors(Function<Element, Stream<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<String> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
