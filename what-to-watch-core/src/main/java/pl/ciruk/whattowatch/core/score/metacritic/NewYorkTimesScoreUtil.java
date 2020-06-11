package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;

import java.util.Optional;

final class NewYorkTimesScoreUtil {
    static final String NEW_YORK_TIMES = "NewYorkTimes";
    private static final int NYT_SCORE_WEIGHT = 10;

    private NewYorkTimesScoreUtil() {
        throw new AssertionError();
    }

    static Optional<Score.ScoreBuilder> extractToScoreBuilder(Element htmlWithScores) {
        return MetacriticSelectors.NEW_YORK_TIMES_GRADE.extractFrom(htmlWithScores)
                .map(grade -> (Double.parseDouble(grade) / 100.0))
                .map(NewYorkTimesScoreUtil::createScore);
    }

    private static Score.ScoreBuilder createScore(Double percentage) {
        return Score.builder()
                .grade(percentage)
                .quantity(NYT_SCORE_WEIGHT)
                .source(NEW_YORK_TIMES)
                .type(ScoreType.CRITIC);
    }
}
