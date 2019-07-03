package pl.ciruk.whattowatch.core.title.onetwothree;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum OneTwoThreeStreamSelectors implements Extractable<Stream<Element>> {
    TITLE_LINKS(description -> description.select("#archive-content .movies .hobcontainer").stream());

    private final Function<Element, Stream<Element>> extractor;

    OneTwoThreeStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
