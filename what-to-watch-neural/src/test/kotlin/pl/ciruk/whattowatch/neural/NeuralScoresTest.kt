package pl.ciruk.whattowatch.neural

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import pl.ciruk.whattowatch.ScoreAssert
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoreType

@Disabled
class NeuralScoresTest {
    companion object {
        private var neuralScores: NeuralScores = NeuralScores(NeuralScores.readDataSet())

        @BeforeAll
        @JvmStatic
        fun setUp() {
            neuralScores.train(2500)
        }
    }

    @Test
    fun shouldScoreSimilarGradeWhenAllScoresAboveSeventy() {
        val scores = createScores(0.72, 0.76, 0.7, 0.77)

        val score = neuralScores.calculateScore(scores)

        ScoreAssert.assertThat(score)
                .hasGradeCloseTo(0.7)
    }

    @Test
    fun shouldScoreSimilarGradeWhenAllScoresAboveNinety() {
        val scores = createScores(0.92, 0.9, 0.9, 0.96)

        val score = neuralScores.calculateScore(scores)

        ScoreAssert.assertThat(score)
                .hasGradeCloseTo(0.9)
    }

    @Test
    fun shouldScoreSimilarGradeWhenAllScoresAreLow() {
        val scores = createScores(0.37, 0.34, 0.35, 0.30)

        val score = neuralScores.calculateScore(scores)

        ScoreAssert.assertThat(score)
                .hasGradeCloseTo(0.3)
    }

    @Test
    fun shouldScoreAtSeventy() {
        val scores = createScores(0.68, 0.57, 0.80, 0.66)

        val score = neuralScores.calculateScore(scores)

        ScoreAssert.assertThat(score)
                .hasGradeCloseTo(0.7)
    }

    private fun createScores(
            filmweb: Double,
            metacritic: Double,
            newYorkTimes: Double,
            imdb: Double): List<Score> {
        return listOf(
                createScore(filmweb, "Filmweb", ScoreType.AMATEUR),
                createScore(metacritic, "Metacritic", ScoreType.CRITIC),
                createScore(newYorkTimes, "New York Times", ScoreType.CRITIC),
                createScore(imdb, "Imdb", ScoreType.AMATEUR)
        )
    }

    private fun createScore(filmweb: Double, source: String, type: ScoreType): Score {
        return Score.builder().source(source).grade(filmweb).quantity(1000).type(type).build()
    }
}