package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.core.score.Score;

import java.util.List;

public record SuggestionResult(
        String title,
        int year,
        String plot,
        String link,
        String poster,
        Double score,
        Double predictedRating,
        List<Score> scores,
        List<String> genres) {
}
