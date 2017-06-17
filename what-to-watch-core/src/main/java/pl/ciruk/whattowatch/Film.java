package pl.ciruk.whattowatch;

import lombok.Builder;
import lombok.Data;
import pl.ciruk.core.math.Doubles;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.ciruk.core.stream.Optionals.asStream;

@Builder
@Data
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
                && scores.size() > 2;
    }

    public Double normalizedScore() {
        Optional<Score> amateur = calculateWeightedAverage(scoresOfType(ScoreType.AMATEUR)).map(Score::amateur);
        Optional<Score> critic = calculateWeightedAverage(scoresOfType(ScoreType.CRITIC)).map(Score::critic);
        Stream<Score> weightedScores = Stream.concat(
                asStream(amateur),
                asStream(critic)
        );

        return calculateWeightedAverage(weightedScores.collect(Collectors.toList()))
                .orElse(0.0);
    }

    private List<Score> scoresOfType(ScoreType type) {
        return scores.stream()
                .filter(score -> score.getType().equals(type))
                .collect(Collectors.toList());
    }

    private Optional<Double> calculateWeightedAverage(List<Score> listOfScores) {
        long totalQuantity = listOfScores.stream()
                .mapToLong(Score::getQuantity)
                .sum();

        double weightedScore = listOfScores.stream()
                .mapToDouble(score -> score.getGrade() * score.getQuantity() / totalQuantity)
                .sum();
        return Optional.of(weightedScore)
                .filter(Doubles.isGreaterThan(0.0));
    }

    public boolean isNotEmpty() {
        return this != EMPTY;
    }
}
