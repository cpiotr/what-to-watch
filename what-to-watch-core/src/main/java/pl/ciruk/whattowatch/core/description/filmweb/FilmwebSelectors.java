package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.text.Patterns;

import java.util.Optional;
import java.util.stream.Stream;

class FilmwebSelectors {
    Stream<String> findLinksFromSearchResult(Element searchResults) {
        return searchResults.select("ul.resultsList li div.filmPreview__card div.filmPreview__header a")
                .stream()
                .map(a -> a.attr("href"));
    }

    Optional<Integer> findYear(Element details) {
        var yearElement = details.selectFirst("span.filmCoverSection__year");
        return Optional.ofNullable(yearElement)
                .map(Element::text)
                .map(text -> Patterns.nonDigit().matcher(text).replaceAll(""))
                .map(Integer::valueOf);
    }

    Optional<String> findLocalTitle(Element details) {
        return Optional.ofNullable(details.selectFirst("h1.filmCoverSection__title"))
                .map(Element::text);
    }

    Optional<String> findOriginalTitle(Element details) {
        return Optional.ofNullable(details.selectFirst("h2.filmCoverSection__orginalTitle"))
                .map(Element::text);
    }

    Optional<String> findPoster(Element details) {
        return Optional.ofNullable(details.selectFirst("div.filmPosterSection__container div.filmPosterSection__poster img"))
                .map(e -> e.attr("src"));
    }

    Optional<String> findPlot(Element details) {
        return Optional.ofNullable(details.selectFirst("div.filmPosterSection__container div.filmPosterSection__plot"))
                .map(Element::text);
    }

    Stream<String> findGenres(Element details) {
        return details.select("div.filmPosterSection__info div.filmInfo__info")
                .stream()
                .filter(div -> div.classNames().size() == 1)
                .findFirst()
                .stream()
                .flatMap(div -> div.select("span a").stream())
                .map(Element::text);
    }
}
