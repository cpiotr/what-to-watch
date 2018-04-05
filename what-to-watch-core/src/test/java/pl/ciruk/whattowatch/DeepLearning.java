package pl.ciruk.whattowatch;

import com.google.common.collect.Lists;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.standalone.ClassPathResource;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.BaseNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.fetcher.BaseDataFetcher;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;

public class DeepLearning {
    public static void main(String[] args) throws IOException, InterruptedException {
        DataSet allData = readDataSet();

        int vectorSize = 4;   //Size of the word vectors. 300 in the Google News model
        int nEpochs = 1;        //Number of epochs (full passes of training data) to train on
        int truncateReviewsToLength = 256;  //Truncate reviews with length (# words) greater than this
        final int seed = 0;     //Seed for reproducibility

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
                .layer(0, new GravesLSTM.Builder().nIn(vectorSize).nOut(128).activation(Activation.TANH).build())
                .layer(1, new GravesLSTM.Builder().nIn(128).nOut(256).activation(Activation.SIGMOID).build())
                .layer(2, new RnnOutputLayer.Builder().activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT).nIn(256).nOut(10).build())
                .pretrain(false).backprop(true).build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(100));

        for (int i = 0; i < 1000; i++) {
            net.fit(allData);
        }

        int[] ints = {66, 70, 90, 72};
        INDArray input = Nd4j.create(1, 4);
        for (int i = 0; i < ints.length; i++) {
            input.putScalar(i, ints[i]);
        }
        INDArray output = net.output(input);
        double score = 0.0;
        for (int i = 0; i < 10; i++) {
            score += (i + 1) * output.getDouble(i);
        }
        System.out.println(score);
    }

    private static DataSet readDataSet() throws IOException, InterruptedException {
        int numLinesToSkip = 1;
        char delimiter = '\t';
        RecordReader recordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        recordReader.initialize(new FileSplit(new ClassPathResource("films.csv").getFile()));

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
