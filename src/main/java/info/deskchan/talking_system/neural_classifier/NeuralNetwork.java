package info.deskchan.talking_system.neural_classifier;

import info.deskchan.core.Path;
import info.deskchan.talking_system.Main;

import java.io.*;
import java.util.*;

public class NeuralNetwork {

    final Random rand = new Random();
    final ArrayList<Neuron> inputLayer = new ArrayList<Neuron>();
    final ArrayList<Neuron> hiddenLayer = new ArrayList<Neuron>();
    final ArrayList<Neuron> outputLayer = new ArrayList<Neuron>();
    final Neuron bias = getNewNeuron();
    int[] layers;
    final int randomWeightMultiplier = 1;

    private int neuronCounter = 0, connectionCounter = 0;

    private final static double epsilon = 0.0000001;

    public double learningRate = 0.002;
    public boolean useDropping = false;
    public boolean storeBest = true;
    double momentum = 0.7;
    double pow = 2;

    private double currentError = Double.MAX_VALUE;

    private Map<String, Integer> inputNeuronsMapping;

    public NeuralNetwork(Path file) throws Exception{
        BufferedReader inputStream = file.newBufferedReader();

        String[] numbers = inputStream.readLine().split(" ");
        buildFromNumbers(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]), Integer.parseInt(numbers[2]));

        String[] weights = inputStream.readLine().split(" ");
        for (Neuron n : hiddenLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet())
                input.getValue().setWeight(Double.parseDouble(weights[input.getValue().id]));

        for (Neuron n : outputLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet())
                input.getValue().setWeight(Double.parseDouble(weights[input.getValue().id]));

        try {
            String[] tags = inputStream.readLine().split(" ");
            setInputNeuronsMapping(Arrays.asList(tags));
        } catch (Exception e){
            Main.log(e);
        }
    }

    public NeuralNetwork(int input, int hidden, int output) {
        buildFromNumbers(input, hidden, output);
    }

    void buildFromNumbers(int input, int hidden, int output) {
        this.layers = new int[] { input, hidden, output };

        /**
         * Create all neurons and connections Connections are created in the
         * neuron class
         */
        for (int i = 0; i < layers.length; i++) {
            if (i == 0) { // input layer
                for (int j = 0; j < layers[i]; j++) {
                    Neuron neuron = getNewNeuron();
                    inputLayer.add(neuron);
                }
            } else if (i == 1) { // hidden layer
                for (int j = 0; j < layers[i]; j++) {
                    Neuron neuron = getNewNeuron();
                    linkNeuronToPastLayer(inputLayer, neuron);
                    addBiasConnection(neuron);
                    hiddenLayer.add(neuron);
                }
            }

            else if (i == 2) { // output layer
                for (int j = 0; j < layers[i]; j++) {
                    Neuron neuron = getNewNeuron();
                    linkNeuronToPastLayer(hiddenLayer, neuron);
                    addBiasConnection(neuron);
                    outputLayer.add(neuron);
                }
            } else {
                System.out.println("Error NeuralNetwork init");
            }
        }

        // initialize random weights
        for (Neuron neuron : hiddenLayer) {
            Collection<Connection> connections = neuron.getAllInConnections();
            for (Connection conn : connections) {
                double newWeight = 0;//getRandom();
                conn.setWeight(newWeight);
            }
        }
        for (Neuron neuron : outputLayer) {
            Collection<Connection> connections = neuron.getAllInConnections();
            for (Connection conn : connections) {
                double newWeight = 0;//getRandom();
                conn.setWeight(newWeight);
            }
        }
        System.out.println(connectionCounter);
    }

    public void setInputNeuronsMapping(Collection<String> tags){
        inputNeuronsMapping = new HashMap<>();
        Iterator<String> it = tags.iterator();
        int i = 0;
        while (it.hasNext()){
            String tag = it.next();
            if (tag.contains(" \n\t"))
                throw new IllegalArgumentException("Tag name should not contain spaces and line endings: "+tag);
            inputNeuronsMapping.put(tag, i);
            i++;
        }
    }

    public Map<String, Integer> getInputNeuronsMapping(){
        return inputNeuronsMapping;
    }

    // random
    private double getRandom() {
        return randomWeightMultiplier * (rand.nextDouble() * 2 - 1); // [-1;1]
    }

    /**
     * @param inputs
     *            There is equally many neurons in the input layer as there are
     *            in input variables
     */
    public void setInputs(double inputs[]) {
        for (int i = 0; i < inputLayer.size(); i++) {
            inputLayer.get(i).setOutput(inputs[i]);
        }
    }


    public void setInputs(Map<String, Double> inputs) {
        setInputsToZero();
        for (Map.Entry<String, Double> entry : inputs.entrySet()){
            setInput(entry.getKey(), entry.getValue());
        }
    }

    public void setInput(int index, double value) {
        inputLayer.get(index).setOutput(value);
    }

    public void setInputsToZero() {
        for (int i = 0; i < inputLayer.size(); i++) {
            inputLayer.get(i).setOutput(0);
        }
    }

    public void setInput(String tag, double value) {
        if (inputNeuronsMapping.containsKey(tag))
            inputLayer.get(inputNeuronsMapping.get(tag)).setOutput(value);
    }

    public double[] getOutput() {
        double[] outputs = new double[outputLayer.size()];
        for (int i = 0; i < outputLayer.size(); i++)
            outputs[i] = outputLayer.get(i).getOutput();
        return outputs;
    }

    /**
     * Calculate the output of the neural network based on the input The forward
     * operation
     */
    public void activate() {
        for (Neuron n : hiddenLayer)
            n.calculateOutput();
        for (Neuron n : outputLayer)
            n.calculateOutput();
    }

    private void linkNeuronToPastLayer(ArrayList<Neuron> inNeurons, Neuron out){
        for(Neuron n : inNeurons){
            Connection con = getNewConnection(n, out);
            out.registerInputConnection(con);
        }
    }

    private void addBiasConnection(Neuron n){
        Connection con = new Connection(bias, n, -1);
        n.biasConnection = con;
    }

    /**
     * all output propagate back
     *
     * @param expectedOutput
     *            first calculate the partial derivative of the error with
     *            respect to each of the weight leading into the output neurons
     *            bias is also updated here
     */
    public void applyBackPropagation(double expectedOutput[], double force) {

        // error check, normalize value ]0;1[
        for (int i = 0; i < expectedOutput.length; i++) {
            double d = expectedOutput[i];
            if (d < 0 || d > 1) {
                if (d < 0)
                    expectedOutput[i] = 0 + epsilon;
                else
                    expectedOutput[i] = 1 - epsilon;
            }
        }

        int i = 0;
        for (Neuron n : outputLayer) {
            Collection<Connection> connections = n.getAllInConnections();
            for (Connection con : connections) {
                double ak = n.getOutput();
                double ai = con.leftNeuron.getOutput();
                double desiredOutput = expectedOutput[i];

                double partialDerivative = -ak * (1 - ak) * ai
                        * (desiredOutput - ak);
                if (partialDerivative == 0)
                    partialDerivative = (desiredOutput - ak);
                double deltaWeight = -learningRate * partialDerivative * force;
                double newWeight = con.getWeight() + deltaWeight;
                con.setDeltaWeight(deltaWeight);
                con.setWeight(newWeight + momentum * con.getPrevDeltaWeight());
            }
            i++;
        }

        // update weights for the hidden layer
        for (Neuron n : hiddenLayer) {
            Collection<Connection> connections = n.getAllInConnections();
            for (Connection con : connections) {
                double aj = n.getOutput();
                double ai = con.leftNeuron.getOutput();
                double sumKoutputs = 0;
                int j = 0;
                for (Neuron out_neu : outputLayer) {
                    double wjk = out_neu.getConnection(n.id).getWeight();
                    double desiredOutput = (double) expectedOutput[j];
                    double ak = out_neu.getOutput();
                    j++;
                    sumKoutputs = sumKoutputs
                            + (-(desiredOutput - ak) * ak * (1 - ak) * wjk);
                }

                double partialDerivative = aj * (1 - aj) * ai * sumKoutputs;
                if (partialDerivative == 0)
                    partialDerivative = sumKoutputs;
                double deltaWeight = -learningRate * partialDerivative * force;
                double newWeight = con.getWeight() + deltaWeight;
                con.setDeltaWeight(deltaWeight);
                con.setWeight(newWeight + momentum * con.getPrevDeltaWeight());
            }
        }
    }

    private Map<Integer, Double> best;
    private double bestError = Double.MAX_VALUE;
    private double lastError = Double.MAX_VALUE;
    private int counter = 0;
    private double maxPerOutpurError = 0;
    private int lastDataSetSize = 0;

    public void learn(DataSet set, int maxSteps, double minError) {
        lastDataSetSize = set.inputs.size();
        int i;
        // Train neural network until minError reached or maxSteps exceeded
        for (i = 0; i < maxSteps && currentError > minError; i++) {
            currentError = 0;
            maxPerOutpurError = 0;

            for (int p = 0; p < set.inputs.size(); p++) {
                learn_Impl(set.inputs.get(p), set.outputs.get(p));
            }
            currentError = currentError / set.inputs.size();
            if (bestError > currentError){
                if (storeBest)
                    best = exportWeights();
                bestError = currentError;
            }
            if (lastError - 0.01 * learningRate <= currentError){
                counter++;
            } else {
                counter = 0;
                lastError = currentError;
            }
            if (useDropping && counter > 50){
                //shock();
                importWeights(best);
                learningRate /= 2;
                counter = 0;
                lastError = Double.MAX_VALUE;
            }
        }

        System.out.println("Current error = " + currentError + ", max deviation: " + maxPerOutpurError +
                ", per output error: " + currentError / getOutputLayerSize() + ", not decreasing sequence: " + counter +
                ", best error: "+bestError + ", learning rate: "+learningRate);
    }

    public void learn(DataSet set) {
        lastDataSetSize = set.inputs.size();
        while(true){
            currentError = 0;
            maxPerOutpurError = 0;

            for (int p = 0; p < set.inputs.size(); p++) {
                learn_Impl(set.inputs.get(p), set.outputs.get(p));
            }
            currentError = currentError / set.inputs.size();
            if (bestError > currentError){
                if (storeBest)
                    best = exportWeights();
                bestError = currentError;
            }
            if (lastError - 0.01 * learningRate <= currentError){
                counter++;
            } else {
                counter = 0;
                lastError = currentError;
            }
            if (useDropping && counter > 10){
                break;
            }
        }
        if (storeBest)
            importWeights(best);

        System.out.println("Current error = " + currentError + ", max deviation: " + maxPerOutpurError +
                ", per output error: " + currentError / getOutputLayerSize() + ", not decreasing sequence: " + counter +
                ", best error: "+bestError);

    }

    public void learnByOne(DataSet set, double maxError) {
        for (int p = 0; p < set.inputs.size(); p++) {
            System.out.println("data line: "+p);
            currentError = Double.MAX_VALUE;
            bestError = Double.MAX_VALUE;
            counter = 0;
            while(currentError > maxError){
                currentError = 0;
                for (int k = 0; k <= p; k++)
                    learn_Impl(set.inputs.get(k), set.outputs.get(k));
                if (bestError > currentError){
                    bestError = currentError;
                }
                counter++;
                if (counter % 100 == 0)
                    System.out.println("Current error = " + currentError + ", best error: "+bestError);
            }
            System.out.println("Round #"+p+" / Current error = " + currentError + ", max deviation: " + maxPerOutpurError +
                    ", per output error: " + currentError / getOutputLayerSize() + ", not decreasing sequence: " + counter +
                    ", best error: "+bestError);
            exportWeights();
        }

    }

    private void learn_Impl(double[] inputs, double[] outputs){
        setInputs(inputs);

        activate();

        double[] output = getOutput();
        double outErr = 0;
        for (int j = 0; j < output.length; j++) {
            double err = Math.pow(Math.abs(output[j] - outputs[j]), pow);
            outErr += err;
            maxPerOutpurError = Math.max(maxPerOutpurError, err);
        }
        currentError += outErr;
        applyBackPropagation(outputs, 1);//outErr);// * k);
    }

    void shock(){
        for (Neuron n : hiddenLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet()) {
                double deltaWeight = -getRandom() * input.getValue().getWeight();
                double newWeight = input.getValue().getWeight() + deltaWeight;
                input.getValue().setDeltaWeight(deltaWeight);
                input.getValue().setWeight(newWeight);
            }

        for (Neuron n : outputLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet()) {
                double deltaWeight = -getRandom() * input.getValue().getWeight();
                double newWeight = input.getValue().getWeight() + deltaWeight;
                input.getValue().setDeltaWeight(deltaWeight);
                input.getValue().setWeight(newWeight);
            }
    }

    String weightKey(int neuronId, int conId) {
        return "N" + neuronId + "_C" + conId;
    }

    public int getInputLayerSize(){ return inputLayer.size(); }

    public int getOutputLayerSize(){ return outputLayer.size(); }

    public int getHiddenLayerSize(){ return hiddenLayer.size(); }

    public String getInputNeuronName(int index){
        for (Map.Entry<String, Integer> entry : inputNeuronsMapping.entrySet())
            if (entry.getValue() == index)
                return entry.getKey();
        throw new IndexOutOfBoundsException("Not such index in input layer: " + index + ", layer size="+inputLayer.size() + ", mapping size: "+inputNeuronsMapping.size());
    }

    public int getInputNeuronIndex(String name){
        return inputNeuronsMapping.getOrDefault(name, -1);
    }

    public double getCurrentError(){ return currentError; }

    private Neuron getNewNeuron(){
        return new Neuron(neuronCounter++);
    }

    private Connection getNewConnection(Neuron from, Neuron to){
        return new Connection(from, to, connectionCounter++);
    }

    public Map<Integer, Double> exportWeights(){
        Map<Integer, Double> weights = new HashMap<>();
        for (Neuron n : hiddenLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet())
                weights.put(input.getValue().id, input.getValue().weight);

        for (Neuron n : outputLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet())
                weights.put(input.getValue().id, input.getValue().weight);
        return weights;
    }

    public void importWeights(Map<Integer, Double> weights){
        for (Neuron n : hiddenLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet())
                input.getValue().setWeight(weights.get(input.getValue().id));

        for (Neuron n : outputLayer)
            for (Map.Entry<Integer, Connection> input : n.inputConnections.entrySet())
                input.getValue().setWeight(weights.get(input.getValue().id));
    }

    public void saveToFile(File file) throws Exception {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8");

        writer.write(getInputLayerSize() + " " + getHiddenLayerSize() + " " + getOutputLayerSize() + " " + bestError + " " + lastDataSetSize + "\n");

        Map<Integer, Double> neuronMap = best != null ? best : exportWeights();

        for (int i = 0; i < neuronMap.size(); i++)
            writer.write((i > 0 ? " " : "") + neuronMap.get(i));
        writer.write("\n");
        for (int i = 0; i < inputLayer.size(); i++){
            writer.write((i > 0 ? " " : "") + getInputNeuronName(i));
        }
        writer.flush();
        writer.close();
    }

}