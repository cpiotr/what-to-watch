package pl.ciruk.whattowatch.title.zalukaj;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum ZalukajStreamSelectors implements Extractable<Stream<Element>> {
    TITLE_LINKS(description -> description.select(".tivief4 .rmk23m4 h3 a")
            .stream()
    );

    private Function<Element, Stream<Element>> extractor;

    ZalukajStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
