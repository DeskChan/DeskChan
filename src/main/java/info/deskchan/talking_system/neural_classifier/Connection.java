package info.deskchan.talking_system.neural_classifier;

import java.util.Random;

public class Connection {
    double weight = 0;
    double prevDeltaWeight = 0; // for momentum
    double deltaWeight = 0;

    final Neuron leftNeuron;
    final Neuron rightNeuron;
    final public int id; // auto increment, starts at 0

    public Connection(Neuron fromN, Neuron toN, int id) {
        leftNeuron = fromN;
        rightNeuron = toN;
        this.id = id;
        weight = new Random().nextDouble() * 2 - 1;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double w) {
        weight = w;
    }

    public void setDeltaWeight(double w) {
        prevDeltaWeight = deltaWeight;
        deltaWeight = w;
    }

    public double getPrevDeltaWeight() {
        return prevDeltaWeight;
    }

    public Neuron getFromNeuron() {
        return leftNeuron;
    }

    public Neuron getToNeuron() {
        return rightNeuron;
    }
}