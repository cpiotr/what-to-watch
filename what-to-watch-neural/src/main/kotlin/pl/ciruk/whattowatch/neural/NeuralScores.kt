package pl.ciruk.whattowatch.neural

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.neuroph.core.data.DataSet
import org.neuroph.core.data.DataSetRow
import org.neuroph.nnet.MultiLayerPerceptron
import org.neuroph.nnet.learning.BackPropagation
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoreType

class NeuralScores(private val dataSet: DataSet) {
    private val network = MultiLayerPerceptron(SOURCES.size, 256, 1)

    @SuppressFBWarnings(justification = "kotlin")
    companion object {
        // The network's inputs, in the order of the training data's columns
        private val SOURCES = listOf("Filmweb", "Metacritic", "NewYorkTimes", "IMDb")

        fun readDataSet(): DataSet {
            val dataSet = DataSet(SOURCES.size, 1)
            val resource = checkNotNull(Thread.currentThread().contextClassLoader.getResource("films.csv")) {
                "Missing training data: films.csv"
            }
            resource.readText()
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

        network.learn(dataSet, backPropagation)
    }

    /**
     * Returns null when any source the network was trained on has no score,
     * since the network can't produce a meaningful result without all its inputs.
     */
    fun calculateScore(scores: List<Score>): Score? {
        val input = SOURCES.map { source -> findGrade(scores, source) ?: return null }

        network.setInput(*input.toDoubleArray())
        network.calculate()
        return Score.builder()
                .grade(network.output.first())
                .quantity(1000)
                .type(ScoreType.CRITIC)
                .source("AI")
                .build()
    }

    private fun findGrade(scores: List<Score>, source: String): Double? {
        return scores.firstOrNull { score -> score.source().equals(source, ignoreCase = true) }?.grade()
    }
}
