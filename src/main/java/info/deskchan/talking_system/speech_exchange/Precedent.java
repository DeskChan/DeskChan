package info.deskchan.talking_system.speech_exchange;

public class Precedent<T extends ICompatible, K> extends InputData<T> {
    public K output = null;

    public Precedent(){}

    public Precedent(float[] coords, T[] history, T input, K output){
        super(coords, history, input);
    }

    public Precedent(InputData<T> copy){
        super(copy);
    }

    @Override
    public String toString() {
        return super.toString() + "Output: " + output + "\n";
    }
}
