package pl.ciruk.whattowatch.core.score.imdb;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import pl.ciruk.whattowatch.utils.net.html.Extractable;
import pl.ciruk.whattowatch.utils.text.Patterns;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public enum ImdbSelectors implements Extractable<Optional<String>> {
    YEAR(result -> Optional.ofNullable(result.selectFirst(".lister-item-header .lister-item-year"))
            .map(Element::text)
            .map(text -> Patterns.nonDigit().matcher(text).replaceAll(""))
            .filter(not(String::isEmpty))),
    TITLE(result -> Optional.ofNullable(result.selectFirst(".lister-item-header a"))
            .map(Element::text)),
    LINK_FROM_SEARCH_RESULT(result -> Optional.ofNullable(result.selectFirst(".lister-item-header a"))
            .filter(element -> element.hasAttr("href"))
            .map(element -> element.attr("href"))),
    ORIGINAL_TITLE(details -> Optional.ofNullable(details.selectFirst(".titleBar .title_wrapper .originalTitle"))
            .stream()
            .map(Element::childNodes)
            .flatMap(List::stream)
            .filter(TextNode.class::isInstance)
            .map(TextNode.class::cast)
            .map(TextNode::text)
            .findFirst()),
    SCORE(result -> result.select(".ratings-bar .ratings-imdb-rating")
            .stream()
            .filter(div -> div.hasAttr("data-value"))
            .map(div -> div.attr("data-value"))
            .filter(not(String::isEmpty))
            .findFirst()),
    NUMBER_OF_SCORES(result -> result.select(".sort-num_votes-visible span")
            .stream()
            .filter(span -> span.hasAttr("data-value"))
            .map(span -> span.attr("data-value"))
            .filter(not(String::isEmpty))
            .findFirst()),
    ;

    private final Function<Element, Optional<String>> extractor;

    ImdbSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }
}
