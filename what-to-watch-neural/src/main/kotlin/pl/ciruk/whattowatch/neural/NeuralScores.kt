package pl.ciruk.whattowatch.neural

import org.datavec.api.records.reader.impl.csv.CSVRecordReader
import org.datavec.api.split.FileSplit
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator
import org.deeplearning4j.nn.conf.GradientNormalization
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.Updater
import org.deeplearning4j.nn.conf.WorkspaceMode
import org.deeplearning4j.nn.conf.layers.GravesLSTM
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.ui.standalone.ClassPathResource
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.api.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
import pl.ciruk.whattowatch.score.Score
import pl.ciruk.whattowatch.score.ScoreType
import java.io.IOException

class NeuralScores(private val dataSet: DataSet) {
    private val network: MultiLayerNetwork

    init {

        val vectorSize = 4
        val seed = 0 //Seed for reproducibility

        //Set up network configuration
        val conf = NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(Updater.ADAM)  //To configure: .updater(Adam.builder().beta1(0.9).beta2(0.999).build())
                .regularization(true).l2(1e-5)
                .weightInit(WeightInit.XAVIER)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue).gradientNormalizationThreshold(1.0)
                .learningRate(2e-2)
                .trainingWorkspaceMode(WorkspaceMode.SEPARATE).inferenceWorkspaceMode(WorkspaceMode.SEPARATE)   //https://deeplearning4j.org/workspaces
                .list()
                .layer(0, GravesLSTM.Builder().nIn(vectorSize).nOut(16).activation(Activation.CUBE).build())
                .layer(1, GravesLSTM.Builder().nIn(16).nOut(256).activation(Activation.SIGMOID).build())
                .layer(2, RnnOutputLayer.Builder().activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT).nIn(256).nOut(10).build())
                .pretrain(false)
                .backprop(true)
                .build()

        network = MultiLayerNetwork(conf)
        network.init()
    }

    fun train(numberOfTrials: Int) {
        for (i in 0 until numberOfTrials) {
            network.fit(dataSet)
        }
    }

    fun calculateScore(scores: List<Score>): Score {
        val sources = arrayOf("Filmweb", "Metacritic", "New York Times", "Imdb")
        val input = Nd4j.create(1, 4)
        for (i in sources.indices) {
            input.putScalar(i, findScore(scores, sources[i]))
        }
        println(input)

        val output = network.output(input)
        println(output)
        var score = 0.0
        for (i in 0..9) {
            score += (i + 1) * output.getDouble(i)
        }
        return Score.builder()
                .grade(score / 10.0)
                .quantity(1000)
                .type(ScoreType.CRITIC)
                .source("AI")
                .build()
    }

    private fun findScore(scores: List<Score>, title: String): Int {
        return scores
                .filter { score -> score.source.equals(title, ignoreCase = true) }
                .map { it.grade }
                .map { grade -> (grade * 100).toInt() }
                .first()
                .or(0)
    }

    companion object {
        fun readDataSet(): DataSet {
            val numLinesToSkip = 1
            val delimiter = '\t'
            val recordReader = CSVRecordReader(numLinesToSkip, delimiter)
            try {
                recordReader.initialize(FileSplit(ClassPathResource("films.csv").file))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            } catch (e: InterruptedException) {
                throw IllegalStateException(e)
            }

            // 5 values in each row: 4 input features followed by an integer label (class) index.
            // Labels are the 5th value (index 4) in each row
            val labelIndex = 4

            // 10 classes: score is an int between 0 and 9
            val numClasses = 10

            // All lines
            val batchSize = 560

            val iterator = RecordReaderDataSetIterator(recordReader, batchSize, labelIndex, numClasses)
            return iterator.next()
        }
    }
}
