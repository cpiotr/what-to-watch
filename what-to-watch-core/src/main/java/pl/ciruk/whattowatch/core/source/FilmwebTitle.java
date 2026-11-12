package pl.ciruk.whattowatch.core.source;

import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;

public record FilmwebTitle(long id, String title, String originalTitle, int year, TitleKind kind) {
    public Title toTitle() {
        return Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .kind(kind)
                .build();
    }
}
