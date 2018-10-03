package info.deskchan.talking_system.intent_exchange;

import info.deskchan.talking_system.CharacterController;

public class Precedent<T extends ICompatible, K> extends InputData<T> {
    public K output;

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
