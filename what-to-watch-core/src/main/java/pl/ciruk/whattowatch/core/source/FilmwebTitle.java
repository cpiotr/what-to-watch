package pl.ciruk.whattowatch.core.source;

import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;

/**
 * A title as Filmweb has it. Its type is what Filmweb calls it in its search API and links: a film, a serial, or a
 * TV programme ("tvshow"), such as "Top Gear", which is a series to everyone else.
 */
public record FilmwebTitle(long id, String title, String originalTitle, int year, TitleKind kind, String type) {
    public FilmwebTitle(long id, String title, String originalTitle, int year, TitleKind kind) {
        this(id, title, originalTitle, year, kind, FilmwebProxy.typesOf(kind).getFirst());
    }

    public Title toTitle() {
        return Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .kind(kind)
                .build();
    }
}
