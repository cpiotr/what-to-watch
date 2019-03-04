package pl.ciruk.whattowatch.core.score.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.Extractable;
import pl.ciruk.whattowatch.utils.stream.Optionals;

import java.util.Optional;
import java.util.function.Function;

public enum FilmwebSelectors implements Extractable<Optional<String>> {
    LINK_FROM_SEARCH_RESULT(result -> result.select(".filmPreview__link")
            .stream()
            .map(link -> link.attr("href"))
            .findFirst()),
    TITLE_FROM_SEARCH_RESULT(result -> result.select(".filmPreview__title")
            .stream()
            .map(Element::text)
            .findFirst()),
    ORIGINAL_TITLE_FROM_SEARCH_RESULT(result -> result.select(".filmPreview__originalTitle")
            .stream()
            .map(Element::text)
            .findFirst()),
    YEAR_FROM_SEARCH_RESULT(result -> result.select(".filmPreview__year")
            .stream()
            .map(Element::text)
            .map(year -> year.replaceAll("\\W", ""))
            .findFirst()),
    SCORE_FROM_DETAILS(details -> extractScoreFromText(details.toString()));

    private final Function<Element, Optional<String>> extractor;

    FilmwebSelectors(Function<Element, Optional<String>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Optional<String> extractFrom(Element details) {
        return extractor.apply(details);
    }

    private static Optional<String> extractScoreFromText(String pageAsText) {
        var ratingValue = extractSpanValueForItem(pageAsText, "ratingValue");
        var ratingCount = extractSpanValueForItem(pageAsText, "ratingCount");
        return Optionals.mergeUsing(
                ratingValue,
                ratingCount,
                (rating, count) -> String.format("%s;%s", rating, count));

    }

    private static Optional<String> extractSpanValueForItem(String pageAsText, String item) {
        var span = "<span itemprop=\"" + item + "\">";
        var fromIndex = pageAsText.indexOf(span);
        if (fromIndex < 0) {
            return Optional.empty();
        }

        fromIndex += span.length();

        var toIndex = pageAsText.indexOf("</span>", fromIndex);
        if (toIndex < fromIndex) {
            return Optional.empty();
        }

        return Optional.of(pageAsText.substring(fromIndex, toIndex));
    }
}
