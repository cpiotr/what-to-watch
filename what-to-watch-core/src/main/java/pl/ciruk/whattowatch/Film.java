package pl.ciruk.whattowatch;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import pl.ciruk.core.math.Doubles;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Data
@EqualsAndHashCode(of = "description")
public class Film {
    static final Film EMPTY = Film.builder().build();

    Description description;

    List<Score> scores;

    public static Film empty() {
        return EMPTY;
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
}
