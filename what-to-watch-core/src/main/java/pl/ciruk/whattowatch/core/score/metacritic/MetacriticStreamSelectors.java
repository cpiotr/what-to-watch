package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum MetacriticStreamSelectors implements Extractable<Stream<Element>> {
    SEARCH_RESULTS(
            page -> page.select("div.body ul.search_results li.result").stream()
    ),
    CRITIC_REVIEWS(
            page -> page.select("div.critic_reviews div.review div.metascore_w").stream()
    );

    private final Function<Element, Stream<Element>> extractor;

    MetacriticStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }


    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
