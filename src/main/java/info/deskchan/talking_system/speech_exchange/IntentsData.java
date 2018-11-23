package info.deskchan.talking_system.speech_exchange;

import info.deskchan.talking_system.IntentList;

public class IntentsData extends IntentList implements IExchangeable {

    public IntentsData(String text){      super(text);  }
    public IntentsData(IntentList list){  super(list);  }

    @Override
    public double checkCompatibility(ICompatible other) {
        if (!(other instanceof IntentsData))
            return 0;

        if (size() == 0) return 1;

        float sum = 0;
        IntentsData o = (IntentsData) other;
        for (String item : this){
            if (o.contains(item))
                sum += 1;
        }
        return sum / size();
    }
}