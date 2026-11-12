package pl.ciruk.whattowatch.core.suggest;

import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.utils.math.Doubles;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ScoredTitle(
        Description description,
        List<Score> scores) {
    private static final ScoredTitle EMPTY = new ScoredTitle(null, null);

    public static ScoredTitle empty() {
        return EMPTY;
    }

    // Amateur and critic scores count by their types' weights, however many votes each has
    public Double normalizedScore() {
        double weightedSum = 0;
        long totalWeight = 0;
        for (var type : ScoreType.values()) {
            var score = normalizedScoreOf(type);
            if (score.isPresent()) {
                weightedSum += score.get() * type.getWeight();
                totalWeight += type.getWeight();
            }
        }
        return Optional.of(weightedSum / totalWeight)
                .filter(Doubles.greaterThan(0.0))
                .orElse(0.0);
    }

    private Optional<Double> normalizedScoreOf(ScoreType type) {
        return calculateWeightedAverage(scoresOfType(type))
                .map(average -> Doubles.normalizeScore(average, countQuantity(type)));
    }

    private long countQuantity(ScoreType type) {
        long totalQuantity = significantScores()
                .filter(score -> score.type() == type)
                .mapToLong(Score::quantity)
                .sum();
        return totalQuantity * type.getScale();
    }

    private List<Score> scoresOfType(ScoreType type) {
        return significantScores()
                .filter(score -> score.type() == type)
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
                .mapToDouble(score -> score.grade() * score.quantity())
                .sum();
        return Optional.of(weightedScore / totalQuantity)
                .filter(Doubles.greaterThan(0.0));
    }
}
