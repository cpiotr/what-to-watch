package pl.ciruk.whattowatch.neural

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.neuroph.core.data.DataSet
import org.neuroph.core.data.DataSetRow
import org.neuroph.nnet.MultiLayerPerceptron
import org.neuroph.nnet.learning.BackPropagation
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoreType

class NeuralScores(private val dataSet: DataSet) {
    private val network = MultiLayerPerceptron(4, 256, 1)

    @SuppressFBWarnings(justification = "kotlin")
    companion object {
        fun readDataSet(): DataSet {
            val dataSet = DataSet(4, 1)
            val resource = Thread.currentThread().contextClassLoader.getResource("films.csv")
            resource!!.readText()
                    .split("\n")
                    .drop(1) // Header
                    .map { it.split("\t") }
                    .map { values -> values.map { it.toDouble() } }
                    .map { doubles -> DataSetRow(extractInput(doubles), extractOutput(doubles)) }
                    .forEach { dataSet.addRow(it) }
            return dataSet
        }

        private fun extractInput(doubles: List<Double>): DoubleArray {
            return doubles.map { it / 100.0 }.dropLast(1).toDoubleArray()
        }

        private fun extractOutput(doubles: List<Double>): DoubleArray {
            return listOf(doubles.last() / 10.0).toDoubleArray()
        }
    }

    fun train(maxIterations: Int) {
        val backPropagation = BackPropagation()
        backPropagation.maxIterations = maxIterations
//        backPropagation.learningRate = 0.05
//        backPropagation.addListener { println((it.source as BackPropagation).totalNetworkError) }

        network.learn(dataSet, backPropagation)
    }

    fun calculateScore(scores: List<Score>): Score {
        val sources = arrayOf("Filmweb", "Metacritic", "New York Times", "Imdb")
        val input = DoubleArray(4)
        for (i in sources.indices) {
            input[i] = findScore(scores, sources[i])
        }
        println(input.toList())

        network.setInput(*input)
        network.calculate()
        val score = network.output.first()
        return Score.builder()
                .grade(score)
                .quantity(1000)
                .type(ScoreType.CRITIC)
                .source("AI")
                .build()
    }

    @SuppressFBWarnings(justification = "kotlin")
    private fun findScore(scores: List<Score>, title: String): Double {
        return scores
                .filter { score -> score.source().equals(title, ignoreCase = true) }
                .map { it.grade() }
                .first()
    }
}
