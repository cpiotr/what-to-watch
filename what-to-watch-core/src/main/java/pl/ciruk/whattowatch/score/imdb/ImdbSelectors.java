package pl.ciruk.whattowatch.score.imdb;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import pl.ciruk.core.net.Extractable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public enum ImdbSelectors implements Extractable<Optional<String>> {
    YEAR(result -> result.select(".lister-item-content .lister-item-header .lister-item-year")
            .stream()
            .map(Element::text)
            .map(text -> text.replaceAll("[^0-9]", ""))
            .findFirst()),
    TITLE(result -> result.select(".lister-item-content .lister-item-header a")
            .stream()
            .map(Element::text)
            .findFirst()),
    LINK_FROM_SEARCH_RESULT(result -> result.select(".lister-item-content .lister-item-header a")
            .stream()
            .filter(element -> element.hasAttr("href"))
            .map(element -> element.attr("href"))
            .findFirst()),
    ORIGINAL_TITLE(details -> details.select(".titleBar .title_wrapper .originalTitle")
            .stream()
            .map(Node::childNodes)
            .flatMap(List::stream)
            .filter(TextNode.class::isInstance)
            .map(TextNode.class::cast)
            .map(TextNode::text)
            .findFirst()),
    SCORE(result -> result.select(".ratings-bar .ratings-imdb-rating")
            .stream()
            .filter(div -> div.hasAttr("data-value"))
            .map(div -> div.attr("data-value"))
            .findFirst()),
    NUMBER_OF_SCORES(result -> result.select(".sort-num_votes-visible span")
            .stream()
            .filter(span -> span.hasAttr("data-value"))
            .map(span -> span.attr("data-value"))
            .findFirst()),;

    private Function<Element, Optional<String>> extractor;

    ImdbSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }
}
