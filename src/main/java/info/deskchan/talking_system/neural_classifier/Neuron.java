package info.deskchan.talking_system.neural_classifier;

import java.util.*;

public class Neuron {

    final int id;  // auto increment, starts at 0
    Connection biasConnection;
    final double bias = -1;
    double output;

    HashMap<Integer, Connection> inputConnections = new HashMap<Integer,Connection>();

    public Neuron(int id){
        this.id = id;
    }

    /**
     * Compute Sj = Wij*Aij + w0j*bias
     */
    public void calculateOutput(){
        double s = 0;
        for(Connection con : inputConnections.values()){
            Neuron leftNeuron = con.getFromNeuron();
            double weight = con.getWeight();
            double a = leftNeuron.getOutput(); //output from previous layer

            s = s + (weight*a);
        }
        s = s + (biasConnection.getWeight()*bias);

        output = g(s);
    }


    double g(double x) {
        return sigmoid(x);
    }

    double sigmoid(double x) {
        return 1.0 / (1.0 +  (Math.exp(-x)));
    }

    public Connection getConnection(int neuronIndex){
        return inputConnections.get(neuronIndex);
    }

    public Collection<Connection> getAllInConnections(){
        return inputConnections.values();
    }

    public double getBias() {
        return bias;
    }

    public double getOutput() {
        return output;
    }

    public void setOutput(double o){
        output = o;
    }

    public void registerInputConnection(Connection connection){
        inputConnections.put(connection.getFromNeuron().id, connection);
    }
}