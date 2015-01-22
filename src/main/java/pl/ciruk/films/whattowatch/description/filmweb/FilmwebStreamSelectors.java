package pl.ciruk.films.whattowatch.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.films.whattowatch.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum FilmwebStreamSelectors implements Extractable<Stream<String>> {
    GENRES(details -> details.select(".filmInfo ul.genresList li")
            .stream()
            .map(Element::text)),
    ;
    private Function<Element, Stream<String>> extractor;

    private FilmwebStreamSelectors(Function<Element, Stream<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<String> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
