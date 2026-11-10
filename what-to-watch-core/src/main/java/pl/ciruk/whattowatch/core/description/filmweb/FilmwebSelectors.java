package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.text.Patterns;

import java.util.Optional;
import java.util.stream.Stream;

class FilmwebSelectors {
    Optional<Integer> findYear(Element details) {
        var yearElement = details.selectFirst(".filmCoverSection__year");
        return Optional.ofNullable(yearElement)
                .map(Element::text)
                .map(text -> Patterns.nonDigit().matcher(text).replaceAll(""))
                .map(Integer::valueOf);
    }

    Optional<String> findLocalTitle(Element details) {
        return Optional.ofNullable(details)
                .map(html -> html.selectFirst("h1.filmCoverSection__title"))
                .map(Element::text);
    }

    Optional<String> findOriginalTitle(Element details) {
        // The year is nested inside the original title, so take only the title's own text
        return Optional.ofNullable(details.selectFirst(".filmCoverSection__originalTitle"))
                .map(Element::ownText);
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
        return details.select("[itemprop=genre]")
                .stream()
                .map(Element::text);
    }
}
