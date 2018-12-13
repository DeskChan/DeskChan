package info.deskchan.talking_system.neural_classifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DataSet {
    int input, output;
    NeuralNetwork network;
    public List<double[]> inputs = new LinkedList<>();
    public List<double[]> outputs = new LinkedList<>();

    public DataSet(NeuralNetwork neuralNetwork){
        network = neuralNetwork;
        input = neuralNetwork.getInputLayerSize();
        output = neuralNetwork.getOutputLayerSize();
    }

    public void addRow(double[] inputs, double[] outputs){
        if (inputs.length < input || outputs.length < output)
            throw new RuntimeException();

        this.inputs.add(inputs);
        this.outputs.add(outputs);
    }

    public void addRow(Map<String, Double> inputs, double[] outputs){
        if (outputs.length < output)
            throw new RuntimeException("Wrong data size. Inputs size: "+inputs.size()+", expected: "+ input + ". Outputs size: "+outputs.length+", expected: "+output);

        double[] inputsArray = new double[input];
        for (int i = 0; i < input; i++)
            inputsArray[i] = 0;

        for (Map.Entry<String, Double> entry : inputs.entrySet()) {
            int pos = network.getInputNeuronIndex(entry.getKey());
            if (pos < 0) continue;
            inputsArray[pos] = entry.getValue();
            //System.out.println(entry.getKey() + " " + network.getInputNeuronIndex(entry.getKey()) + " " + entry.getValue() + " " +  inputsArray[network.getInputNeuronIndex(entry.getKey())]);
        }

        this.inputs.add(inputsArray);
        this.outputs.add(outputs);

        /*for (int i = 0; i < inputsArray.length; i++)
            if (inputsArray[i] != 0)
                System.out.print(i + ": " + inputsArray[i] + ", ");
        System.out.println();
        for (int i = 0; i < outputs.length; i++)
            System.out.print(outputs[i] + " ");
        System.out.println();*/
    }
}
