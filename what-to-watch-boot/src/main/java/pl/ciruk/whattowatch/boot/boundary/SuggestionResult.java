package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.title.TitleKind;

import java.util.List;

// The local and original titles, year and kind identify the title when I rate it
public record SuggestionResult(
        String title,
        String localTitle,
        String originalTitle,
        int year,
        TitleKind kind,
        String plot,
        String link,
        String poster,
        Double score,
        Double predictedRating,
        List<Score> scores,
        List<String> genres,
        Integer myRating) {
}
