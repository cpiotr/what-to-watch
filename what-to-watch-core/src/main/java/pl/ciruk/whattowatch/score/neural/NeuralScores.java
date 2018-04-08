package pl.ciruk.whattowatch.score.neural;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.standalone.ClassPathResource;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreType;

import java.io.IOException;
import java.util.List;

public class NeuralScores {
    private final DataSet dataSet;
    private final MultiLayerNetwork network;

    public NeuralScores(DataSet dataSet) {
        this.dataSet = dataSet;

        int vectorSize = 4;
        final int seed = 0; //Seed for reproducibility

        //Set up network configuration
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(Updater.ADAM)  //To configure: .updater(Adam.builder().beta1(0.9).beta2(0.999).build())
                .regularization(true).l2(1e-5)
                .weightInit(WeightInit.XAVIER)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue).gradientNormalizationThreshold(1.0)
                .learningRate(2e-2)
                .trainingWorkspaceMode(WorkspaceMode.SEPARATE).inferenceWorkspaceMode(WorkspaceMode.SEPARATE)   //https://deeplearning4j.org/workspaces
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(vectorSize).nOut(16).activation(Activation.CUBE).build())
                .layer(1, new GravesLSTM.Builder().nIn(16).nOut(256).activation(Activation.SIGMOID).build())
                .layer(2, new RnnOutputLayer.Builder().activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT).nIn(256).nOut(10).build())
                .pretrain(false)
                .backprop(true)
                .build();

        network = new MultiLayerNetwork(conf);
        network.init();
    }

    public void train(int numberOfTrials) {
        for (int i = 0; i < numberOfTrials; i++) {
            network.fit(dataSet);
        }
    }

    public Score calculateScore(List<Score> scores) {
        String[] sources = {"Filmweb", "Metacritic", "New York Times", "Imdb"};
        INDArray input = Nd4j.create(1, 4);
        for (int i = 0; i < sources.length; i++) {
            input.putScalar(i, findScore(scores, sources[i]));
        }
        System.out.println(input);

        INDArray output = network.output(input);
        System.out.println(output);
        double score = 0.0;
        for (int i = 0; i < 10; i++) {
            score += (i + 1) * output.getDouble(i);
        }
        return Score.builder()
                .grade(score / 10.0)
                .quantity(1000)
                .type(ScoreType.CRITIC)
                .source("AI")
                .build();
    }

    private int findScore(List<Score> scores, String title) {
        return scores.stream()
                .filter(score -> score.getSource().equalsIgnoreCase(title))
                .map(Score::getGrade)
                .map(grade -> (int) (grade * 100))
                .findFirst()
                .orElse(0);
    }

    public static DataSet readDataSet() {
        int numLinesToSkip = 1;
        char delimiter = '\t';
        RecordReader recordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        try {
            recordReader.initialize(new FileSplit(new ClassPathResource("films.csv").getFile()));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        // 5 values in each row: 4 input features followed by an integer label (class) index.
        // Labels are the 5th value (index 4) in each row
        int labelIndex = 4;

        // 10 classes: score is an int between 0 and 9
        int numClasses = 10;

        // All lines
        int batchSize = 560;

        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, batchSize, labelIndex, numClasses);
        return iterator.next();
    }
}
