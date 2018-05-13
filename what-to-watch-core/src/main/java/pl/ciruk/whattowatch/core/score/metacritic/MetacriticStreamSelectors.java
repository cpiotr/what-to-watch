package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum MetacriticStreamSelectors implements Extractable<Stream<Element>> {
    SEARCH_RESULTS(
            page -> page.select("div.body .search_results .result").stream()
    ),
    CRITIC_REVIEWS(
            page -> page.select(".critic_reviews .review .metascore_w").stream()
    )
    ;

    private Function<Element, Stream<Element>> extractor;

    MetacriticStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }


    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }
}
