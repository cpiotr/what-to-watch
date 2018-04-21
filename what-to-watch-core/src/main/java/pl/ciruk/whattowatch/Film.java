package pl.ciruk.whattowatch;

import pl.ciruk.core.math.Doubles;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Film {
    static final Film EMPTY = Film.builder().build();

    Description description;

    List<Score> scores;

    private Film(Description description, List<Score> scores) {
        this.description = description;
        this.scores = scores;
    }

    public static Film empty() {
        return EMPTY;
    }

    public static FilmBuilder builder() {
        return new FilmBuilder();
    }

    public boolean isWorthWatching() {
        return isNotEmpty()
                && normalizedScore() > 0.65
                && !scoresOfType(ScoreType.AMATEUR).isEmpty()
                && !scoresOfType(ScoreType.CRITIC).isEmpty();
    }

    public Double normalizedScore() {
        Optional<Score> amateur = calculateWeightedAverage(scoresOfType(ScoreType.AMATEUR))
                .map(score -> Doubles.normalizeScore(score, countQuantity(ScoreType.AMATEUR)))
                .map(Score::amateur);
        Optional<Score> critic = calculateWeightedAverage(scoresOfType(ScoreType.CRITIC))
                .map(score -> Doubles.normalizeScore(score, countQuantity(ScoreType.CRITIC)))
                .map(Score::critic);
        Stream<Score> weightedScores = Stream.concat(
                amateur.stream(),
                critic.stream()
        );

        return calculateWeightedAverage(weightedScores.collect(Collectors.toList()))
                .orElse(0.0);
    }

    private long countQuantity(ScoreType type) {
        return significantScores()
                .filter(score -> score.getType().equals(type))
                .mapToLong(Score::getQuantity)
                .sum();
    }

    private List<Score> scoresOfType(ScoreType type) {
        return significantScores()
                .filter(score -> score.getType().equals(type))
                .collect(Collectors.toList());
    }

    private Stream<Score> significantScores() {
        return scores.stream()
                .filter(Score::isSignificant);
    }

    private static Optional<Double> calculateWeightedAverage(List<Score> listOfScores) {
        long totalQuantity = listOfScores.stream()
                .mapToLong(Score::getQuantity)
                .sum();

        double weightedScore = listOfScores.stream()
                .mapToDouble(score -> score.getGrade() * score.getQuantity())
                .sum();
        return Optional.of(weightedScore / totalQuantity)
                .filter(Doubles.isGreaterThan(0.0));
    }

    public boolean isNotEmpty() {
        return this != EMPTY;
    }

    public Description getDescription() {
        return this.description;
    }

    public List<Score> getScores() {
        return this.scores;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public void setScores(List<Score> scores) {
        this.scores = scores;
    }

    public String toString() {
        return "Film(description=" + this.getDescription() + ", scores=" + this.getScores() + ")";
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Film)) return false;
        final Film other = (Film) o;
        if (!other.canEqual(this)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        return this$description == null ? other$description == null : this$description.equals(other$description);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    private boolean canEqual(Object other) {
        return other instanceof Film;
    }

    public static class FilmBuilder {
        private Description description;
        private List<Score> scores;

        FilmBuilder() {
        }

        public Film.FilmBuilder description(Description description) {
            this.description = description;
            return this;
        }

        public Film.FilmBuilder scores(List<Score> scores) {
            this.scores = scores;
            return this;
        }

        public Film build() {
            return new Film(description, scores);
        }

        public String toString() {
            return "Film.FilmBuilder(description=" + this.description + ", scores=" + this.scores + ")";
        }
    }
}
