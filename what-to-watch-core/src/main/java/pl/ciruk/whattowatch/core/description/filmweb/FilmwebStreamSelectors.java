package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum FilmwebStreamSelectors implements Extractable<Stream<String>> {
    GENRES(details -> details.select("div.filmPosterSection__info div.filmInfo__info")
            .stream()
            .filter(div -> div.classNames().size() == 1)
            .findFirst()
            .stream()
            .flatMap(div -> div.select("span a").stream())
            .map(Element::text)),
    LINKS_FROM_SEARCH_RESULT(page -> page.select("ul.resultsList li div.filmPreview__card div.filmPreview__header a")
            .stream()
            .map(a -> a.attr("href"))),
    ;
    private final Function<Element, Stream<String>> extractor;

    FilmwebStreamSelectors(Function<Element, Stream<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<String> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
