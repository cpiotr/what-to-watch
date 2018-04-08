package pl.ciruk.whattowatch.score.neural;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreAssert;
import pl.ciruk.whattowatch.score.ScoreType;

import java.util.List;

public class NeuralScoresTest {

    private static NeuralScores neuralScores;

    @BeforeClass
    public static void setUp() {
        neuralScores = new NeuralScores(NeuralScores.readDataSet());
        neuralScores.train(500);
    }

    @Test
    public void shouldScoreSimilarGradeWhenAllScoresAboveSeventy() {
        List<Score> scores = createScores(0.72, 0.76, 0.7, 0.77);

        Score score = neuralScores.calculateScore(scores);

        ScoreAssert.assertThat(score)
                .hasGradeGreaterThan(0.7)
                .hasGradeLessThan(0.8);
    }

    @Test
    public void shouldScoreSimilarGradeWhenAllScoresAboveNinety() {
        List<Score> scores = createScores(0.92, 0.9, 0.9, 0.96);

        Score score = neuralScores.calculateScore(scores);

        ScoreAssert.assertThat(score)
                .hasGradeGreaterThan(0.9)
                .hasGradeLessThan(1.0);
    }

    @Test
    public void shouldScoreSimilarGradeWhenAllScoresAreLow() {
        List<Score> scores = createScores(0.37, 0.34, 0.35, 0.30);

        Score score = neuralScores.calculateScore(scores);

        ScoreAssert.assertThat(score)
                .hasGradeGreaterThan(0.3)
                .hasGradeLessThan(0.4);
    }

    private List<Score> createScores(
            double filmweb,
            double metacritic,
            double newYorkTimes,
            double imdb) {
        return List.of(
                createScore(filmweb, "Filmweb", ScoreType.AMATEUR),
                createScore(metacritic, "Metacritic",ScoreType.CRITIC),
                createScore(newYorkTimes, "New York Times",ScoreType.CRITIC),
                createScore(imdb, "Imdb", ScoreType.AMATEUR)
        );
    }

    private Score createScore(double filmweb, String source, ScoreType type) {
        return Score.builder().source(source).grade(filmweb).quantity(1000).type(type).build();
    }
}