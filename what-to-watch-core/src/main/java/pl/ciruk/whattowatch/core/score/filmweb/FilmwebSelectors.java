package pl.ciruk.whattowatch.core.score.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;
import pl.ciruk.whattowatch.utils.stream.Optionals;
import pl.ciruk.whattowatch.utils.text.Patterns;

import java.util.Optional;
import java.util.function.Function;

public enum FilmwebSelectors implements Extractable<Optional<String>> {
    LINK_FROM_SEARCH_RESULT(result -> Optional.ofNullable(result.selectFirst("a.filmPreview__link"))
            .map(link -> link.attr("href"))),
    TITLE_FROM_SEARCH_RESULT(result -> Optional.ofNullable(result.selectFirst("h3.filmPreview__title"))
            .map(Element::text)),
    ORIGINAL_TITLE_FROM_SEARCH_RESULT(result -> Optional.ofNullable(result.selectFirst("div.filmPreview__originalTitle"))
            .map(Element::text)),
    YEAR_FROM_SEARCH_RESULT(result -> Optional.ofNullable(result.selectFirst("span.filmPreview__year"))
            .map(Element::text)
            .map(year -> Patterns.nonDigit().matcher(year).replaceAll(""))),
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
