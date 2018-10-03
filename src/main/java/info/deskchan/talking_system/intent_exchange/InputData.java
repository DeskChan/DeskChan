package info.deskchan.talking_system.intent_exchange;

import info.deskchan.talking_system.CharacterController;
import info.deskchan.talking_system.CharacterRange;

import java.util.List;

public class InputData<T extends ICompatible>{

    public float[] coords = null;
    // last means latest
    public T[] history = null;
    public T input;

    public InputData(InputData<T> copy){
        this.coords = copy.coords;
        this.history = copy.history;
        this.input = copy.input;
    }

    public InputData(T[] history, T input){
        this.input = input;
        this.history = history;
    }

    public InputData(float[] coords, T[] history, T input){
        this.input = input;
        this.history = history;
    }

    private static final double INPUT_WEIGHT = 1, HISTORY_WEIGHT = 1;

    // if other is compatible with this
    public float checkCompatible(InputData<T> other){
        float sum = 0;
        sum += input.checkCompatibility(other.input) * INPUT_WEIGHT;

        if (history != null && history.length > 0){
            float sim = 0, simALl = 0;
            for (int i = 0; i < history.length; i++){
                float mul = (history.length - i);
                if (i < other.history.length)
                    sim += mul * history[i].checkCompatibility(other.history[i]);
                simALl += mul;
            }
            sum += sim / simALl * HISTORY_WEIGHT;
        } else {
            sum += HISTORY_WEIGHT;
        }
        sum /= INPUT_WEIGHT + HISTORY_WEIGHT;
        return sum;
    }

    public void setCoords(List<float[]>... coords){
        float[] single = new float[CharacterRange.getFeatureCount()];
        this.coords = new float[coords.length * single.length];

        for (int i = 0; i < coords.length; i++){
            for (int j = 0; j < single.length; j++){
                this.coords[i*single.length + j] = single[j] = 0;
            }
            if (coords[i].size() == 0) continue;
            for (int j = 0; j < coords[i].size(); j++){
                for (int k = 0; k < single.length; k++) {
                    single[k] += coords[i].get(j)[k] * (coords[i].size() - j);
                }
            }
            for (int j = 0; j < single.length; j++){
                this.coords[i*single.length + j] = single[j] / ((2+coords[i].size()) * coords[i].size() / 2);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vector: {" + coords[0]);
        for (int i = 1; i < coords.length; i++)
            sb.append(", " + coords[i]);
        sb.append(" / Input: [0]="+input.toString()+"\n");
        for (int i = 0; i < history.length; i++)
            sb.append("[" + (i+1) + "]=" + history[i].toString()+"\n");
        return sb.toString();
    }
}
