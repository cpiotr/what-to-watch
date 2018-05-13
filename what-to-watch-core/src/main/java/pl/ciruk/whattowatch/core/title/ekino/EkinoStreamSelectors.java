package pl.ciruk.whattowatch.core.title.ekino;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum EkinoStreamSelectors implements Extractable<Stream<Element>> {
    TITLE_LINKS(description -> description.select(".mainWrap .movies-list-item").stream());

    private Function<Element, Stream<Element>> extractor;

    EkinoStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
