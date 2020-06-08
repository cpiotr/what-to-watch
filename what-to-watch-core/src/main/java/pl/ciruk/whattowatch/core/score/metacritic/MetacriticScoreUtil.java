package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.utils.math.Doubles;

import java.util.Optional;

import static pl.ciruk.whattowatch.utils.stream.Optionals.mergeUsing;

final class MetacriticScoreUtil {
    static final String METACRITIC = "Metacritic";

    private MetacriticScoreUtil() {
        throw new AssertionError();
    }

    static Optional<Score.ScoreBuilder> extractToScoreBuilder(Element htmlWithScores) {
        var averageGrade = averageGradeFrom(htmlWithScores);
        var numberOfReviews = numberOfReviewsFrom(htmlWithScores);
        return mergeUsing(
                averageGrade,
                numberOfReviews,
                MetacriticScoreUtil::createScore);
    }

    private static Optional<Double> averageGradeFrom(Element htmlWithScores) {
        return MetacriticStreamSelectors.CRITIC_REVIEWS.extractFrom(htmlWithScores)
                .map(Element::text)
                .mapToDouble(Double::valueOf)
                .average()
                .stream()
                .map(grade -> grade / 100.0)
                .boxed()
                .findFirst();
    }

    private static Optional<Double> numberOfReviewsFrom(Element htmlWithScores) {
        double count = MetacriticStreamSelectors.CRITIC_REVIEWS.extractFrom(htmlWithScores)
                .map(Element::text)
                .mapToDouble(Double::valueOf)
                .count();
        return Optional.of(count).filter(Doubles.greaterThan(0.0));
    }

    private static Score.ScoreBuilder createScore(Double rating, Double count) {
        return Score.builder()
                .grade(rating)
                .quantity(count.intValue())
                .source(METACRITIC)
                .type(ScoreType.CRITIC);
    }
}
