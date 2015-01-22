package pl.ciruk.films.whattowatch.title.ekino;

import org.jsoup.nodes.Element;
import pl.ciruk.films.whattowatch.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum EkinoStreamSelectors implements Extractable<Stream<Element>> {
    FILMS(page -> page.select("ul.movies li")
            .stream()),

    ;

    private Function<Element, Stream<Element>> extractor;

    private EkinoStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }


    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
