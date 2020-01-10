package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.core.score.Score;

import java.util.List;
import java.util.Objects;

class FilmResult {
    private final String title;
    private final Integer year;
    private final String plot;
    private final String link;
    private final String poster;
    private final Double score;
    private final Integer numberOfScores;
    private final List<Score> scores;
    private final List<String> genres;

    private FilmResult(
            String title,
            Integer year,
            String plot,
            String link,
            String poster,
            Double score,
            Integer numberOfScores,
            List<Score> scores,
            List<String> genres) {
        this.title = title;
        this.year = year;
        this.plot = plot;
        this.link = link;
        this.poster = poster;
        this.score = score;
        this.numberOfScores = numberOfScores;
        this.scores = scores;
        this.genres = genres;
    }

    static FilmResultBuilder builder() {
        return new FilmResultBuilder();
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    public String getPlot() {
        return plot;
    }

    public String getLink() {
        return link;
    }

    public String getPoster() {
        return poster;
    }

    public Double getScore() {
        return score;
    }

    public Integer getNumberOfScores() {
        return numberOfScores;
    }

    public List<Score> getScores() {
        return scores;
    }

    public List<String> getGenres() {
        return genres;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        FilmResult other = (FilmResult) object;
        return Objects.equals(title, other.title) &&
                Objects.equals(year, other.year) &&
                Objects.equals(link, other.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year, link);
    }

    static class FilmResultBuilder {
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
            return new FilmResult(title, year, plot, link, poster, score, numberOfScores, scores, genres);
        }
    }
}
