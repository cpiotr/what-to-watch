package pl.ciruk.whattowatch.core.suggest;

import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.utils.math.Doubles;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("PMD.TooManyMethods")
public record Film(
        Description description,
        List<Score>scores) {
    private static final Film EMPTY = Film.builder().build();

    public static Film empty() {
        return EMPTY;
    }

    public static FilmBuilder builder() {
        return new FilmBuilder();
    }

    public Double normalizedScore() {
        var amateurScore = calculateWeightedAverage(scoresOfType(ScoreType.AMATEUR))
                .map(score -> Doubles.normalizeScore(score, countQuantity(ScoreType.AMATEUR)))
                .map(Score::amateur);
        var criticScore = calculateWeightedAverage(scoresOfType(ScoreType.CRITIC))
                .map(score -> Doubles.normalizeScore(score, countQuantity(ScoreType.CRITIC)))
                .map(Score::critic);
        var weightedScores = Stream.concat(
                amateurScore.stream(),
                criticScore.stream()
        );

        return calculateWeightedAverage(weightedScores.collect(Collectors.toList()))
                .orElse(0.0);
    }

    private long countQuantity(ScoreType type) {
        long totalQuantity = significantScores()
                .filter(score -> score.type().equals(type))
                .mapToLong(Score::quantity)
                .sum();
        return totalQuantity * type.getScale();
    }

    private List<Score> scoresOfType(ScoreType type) {
        return significantScores()
                .filter(score -> score.type().equals(type))
                .collect(Collectors.toList());
    }

    private Stream<Score> significantScores() {
        return scores.stream()
                .filter(Score::isSignificant);
    }

    public boolean isNotEmpty() {
        return this != EMPTY;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    private static Optional<Double> calculateWeightedAverage(List<Score> listOfScores) {
        var totalQuantity = listOfScores.stream()
                .mapToLong(Score::quantity)
                .sum();

        var weightedScore = listOfScores.stream()
                .mapToDouble(score -> score.grade() * score.grade())
                .sum();
        return Optional.of(weightedScore / totalQuantity)
                .filter(Doubles.greaterThan(0.0));
    }

    public static class FilmBuilder {
        private Description description;
        private List<Score> scores;

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

        @Override
        public String toString() {
            return "Film.FilmBuilder(description=" + this.description + ", scores=" + this.scores + ")";
        }
    }
}
