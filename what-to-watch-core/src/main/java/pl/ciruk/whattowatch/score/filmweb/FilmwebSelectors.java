package pl.ciruk.whattowatch.score.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;
import pl.ciruk.core.stream.Optionals;

import java.util.Optional;
import java.util.function.Function;

public enum FilmwebSelectors implements Extractable<Optional<String>> {
    LINK_FROM_SEARCH_RESULT(result -> result.select(".hitDescWrapper a.hitTitle")
            .stream()
            .map(link -> link.attr("href"))
            .findFirst()),
    TITLE_FROM_SEARCH_RESULT(result -> result.select(".hitDescWrapper a.hitTitle")
            .stream()
            .map(Element::text)
            .findFirst()),
    YEAR_FROM_SEARCH_RESULT(result -> result.select(".hitDescWrapper span.hitTitle")
            .stream()
            .map(Element::text)
            .map(year -> year.replaceAll("\\W", ""))
            .findFirst()),
    SCORE_FROM_SEARCH_RESULT(result -> result.select(".rateInfo .box")
            .stream()
            .map(Element::text)
            .findFirst()),
    SCORE_FROM_DETAILS(details -> extractScoreFromText(details.toString()));

    private Function<Element, Optional<String>> extractor;

    FilmwebSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }

    private static Optional<String> extractScoreFromText(String pageAsText) {
        Optional<String> ratingValue = extractSpanValueForItem(pageAsText, "ratingValue");
        Optional<String> ratingCount = extractSpanValueForItem(pageAsText, "ratingCount");
        return Optionals.mergeUsing(
                ratingValue,
                ratingCount,
                (rating, count) -> String.format("%s;%s", rating, count));

    }

    private static Optional<String> extractSpanValueForItem(String pageAsText, String item) {
        String span = "<span itemprop=\"" + item + "\">";
        int from = pageAsText.indexOf(span);
        if (from < 0) {
            return Optional.empty();
        }

        from += span.length();
        int to = pageAsText.indexOf("</span>", from);
        if (to < from) {
            return Optional.empty();
        }

        return Optional.of(pageAsText.substring(from, to));
    }
}
