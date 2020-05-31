package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.core.score.Score;

import java.util.List;
import java.util.Objects;

public record FilmResult(
        String id,
        String title,
        Integer year,
        String plot,
        String link,
        String poster,
        Double score,
        Integer numberOfScores,
        List<Score>scores,
        List<String>genres) {
    static FilmResultBuilder builder() {
        return new FilmResultBuilder();
    }

    static class FilmResultBuilder {
        private String id;
        private String title;
        private Integer year;
        private String plot;
        private String link;
        private String poster;
        private Double score;
        private Integer numberOfScores;
        private List<Score> scores;
        private List<String> genres;

        FilmResultBuilder title(String title) {
            this.title = title;
            return this;
        }

        FilmResultBuilder id(String id) {
            this.id = id;
            return this;
        }

        FilmResultBuilder year(Integer year) {
            this.year = year;
            return this;
        }

        FilmResultBuilder plot(String plot) {
            this.plot = plot;
            return this;
        }

        FilmResultBuilder link(String link) {
            this.link = link;
            return this;
        }

        FilmResultBuilder poster(String poster) {
            this.poster = poster;
            return this;
        }

        FilmResultBuilder score(Double score) {
            this.score = score;
            return this;
        }

        FilmResultBuilder numberOfScores(Integer numberOfScores) {
            this.numberOfScores = numberOfScores;
            return this;
        }

        FilmResultBuilder scores(List<Score> scores) {
            this.scores = scores;
            return this;
        }

        FilmResultBuilder genres(List<String> genres) {
            this.genres = genres;
            return this;
        }

        FilmResult build() {
            var id = this.id == null
                    ? String.valueOf(Objects.hash(title, year, link))
                    : this.id;
            return new FilmResult(id, title, year, plot, link, poster, score, numberOfScores, scores, genres);
        }
    }
}
