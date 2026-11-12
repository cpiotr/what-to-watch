package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.nodes.Element;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class FilmwebSelectors {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    // A series has the years it ran, such as "2022 - 2026", and is known by the first one
    Optional<Integer> findYear(Element details) {
        return Optional.ofNullable(details.selectFirst(".filmCoverSection__year"))
                .flatMap(year -> YEAR.matcher(year.text()).results().findFirst())
                .map(match -> Integer.valueOf(match.group()));
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

    // The plot sits next to a "see full description" link; a film without a plot has only a prompt to add one
    Optional<String> findPlot(Element details) {
        return Optional.ofNullable(details.selectFirst("div.filmPosterSection__container div.filmPosterSection__plot [itemprop=description]"))
                .map(Element::text);
    }

    Stream<String> findGenres(Element details) {
        return details.select("[itemprop=genre]")
                .stream()
                .map(Element::text);
    }
}
