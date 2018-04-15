package pl.ciruk.whattowatch.neural

import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import pl.ciruk.whattowatch.ScoreAssert
import pl.ciruk.whattowatch.score.Score
import pl.ciruk.whattowatch.score.ScoreType

@Ignore
class NeuralScoresTest {
    companion object {
        private var neuralScores: NeuralScores = NeuralScores(NeuralScores.readDataSet())
        @BeforeClass
        fun setUp() {
            neuralScores.train(500)
        }
    }

    @Test
    fun shouldScoreSimilarGradeWhenAllScoresAboveSeventy() {
        val scores = createScores(0.72, 0.76, 0.7, 0.77)

        val score = neuralScores.calculateScore(scores)

        ScoreAssert.assertThat(score)
                .hasGradeGreaterThan(0.7)
                .hasGradeLessThan(0.8)
    }

    @Test
    fun shouldScoreSimilarGradeWhenAllScoresAboveNinety() {
        val scores = createScores(0.92, 0.9, 0.9, 0.96)

        val score = neuralScores.calculateScore(scores)

        ScoreAssert.assertThat(score)
                .hasGradeGreaterThan(0.9)
                .hasGradeLessThan(1.0)
    }

    @Test
    fun shouldScoreSimilarGradeWhenAllScoresAreLow() {
        val scores = createScores(0.37, 0.34, 0.35, 0.30)

        val score = neuralScores.calculateScore(scores)

        ScoreAssert.assertThat(score)
                .hasGradeGreaterThan(0.3)
                .hasGradeLessThan(0.4)
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