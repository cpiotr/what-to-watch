package pl.ciruk.whattowatch.neural

import org.neuroph.core.data.DataSet
import org.neuroph.core.data.DataSetRow
import org.neuroph.nnet.MultiLayerPerceptron
import org.neuroph.nnet.learning.BackPropagation
import pl.ciruk.whattowatch.score.Score
import pl.ciruk.whattowatch.score.ScoreType


class NeuralScores(private val dataSet: DataSet) {
    private val network = MultiLayerPerceptron(4, 256, 1)

    companion object {
        fun readDataSet(): DataSet {
            val dataSet = DataSet(4, 1)
            val resource = Thread.currentThread().contextClassLoader.getResource("films.csv")
            resource.readText().split("\n")
                    .drop(1) // Header
                    .map { it.split("\t") }
                    .map { values -> values.map { it.toDouble() }.toDoubleArray() }
                    .map { doubles -> DataSetRow(withoutLast(doubles), onlyLast(doubles)) }
                    .forEach { dataSet.addRow(it) }
            return dataSet
        }

        private fun withoutLast(doubles: DoubleArray): ArrayList<Double> {
            val arrayList = ArrayList(doubles.map { it / 100.0 }.slice(0 until doubles.size - 1))
            println(arrayList)
            return arrayList
        }

        private fun onlyLast(doubles: DoubleArray): ArrayList<Double> {
            val doubleArray = DoubleArray(10)
            doubleArray[doubles.last().toInt()] = 1.0
            val arrayList = ArrayList(listOf(doubles.last() / 10.0))
            println(arrayList)
            return arrayList
        }
    }

    fun train(maxIterations: Int) {
        val backPropagation = BackPropagation()
        backPropagation.maxIterations = maxIterations
        backPropagation.learningRate = 0.2

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

    private fun findScore(scores: List<Score>, title: String): Double {
        return scores
                .filter { score -> score.source.equals(title, ignoreCase = true) }
                .map { it.grade }
                .first()
    }
}
