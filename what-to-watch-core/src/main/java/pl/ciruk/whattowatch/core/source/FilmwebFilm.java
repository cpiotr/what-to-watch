package pl.ciruk.whattowatch.core.source;

import pl.ciruk.whattowatch.core.title.Title;

public record FilmwebFilm(long id, String title, String originalTitle, int year) {
    public Title toTitle() {
        return Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .build();
    }
}
