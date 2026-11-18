package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.suggest.ScoredTitle;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.ratings.RatedTitle;

import java.util.List;
import java.util.Objects;

/**
 * My rating of a suggestion, with the scores the suggestion was shown with, which the personal model learns from.
 */
public record RatingRequest(
        String title,
        String originalTitle,
        int year,
        TitleKind kind,
        int rating,
        List<Score> scores) {

    RatedTitle toRatedTitle() {
        var rated = Title.builder()
                .title(Ratings.blankAsNull(title))
                .originalTitle(Ratings.blankAsNull(originalTitle))
                .year(year)
                .kind(Objects.requireNonNullElse(kind, TitleKind.FILM))
                .build();
        var scored = new ScoredTitle(Description.builder().title(rated).build(), Objects.requireNonNullElse(scores, List.of()));
        return new RatedTitle(scored, rating);
    }
}
