package pl.ciruk.whattowatch.neural

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.neuroph.core.data.DataSet
import pl.ciruk.whattowatch.core.score.Score

class NeuralScoresInputTest {
    private val neuralScores = NeuralScores(DataSet(4, 1))

    @Test
    fun shouldGiveNoScoreWhenSourceIsMissing() {
        val scores = listOf(
                Score.amateur(0.7, 1000).withSource("Filmweb"),
                Score.critic(0.7, 40).withSource("Metacritic"))

        assertThat(neuralScores.calculateScore(scores)).isNull()
    }

    private fun Score.withSource(source: String): Score =
            Score.builder().grade(grade()).quantity(quantity()).type(type()).source(source).build()
}
