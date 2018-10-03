package info.deskchan.talking_system.intent_exchange;

import java.util.*;

public class IntentList extends ArrayList<String> implements ICompatible {

    public IntentList(){ super(); }

    public IntentList(Collection<String> intents){ super(new HashSet<>(intents)); }

    public IntentList(String[] intents){
        super();
        for (String item : intents) add(item);
    }

    @Override
    public boolean add(String element) {
        if (!contains(element))
            return super.add(element);
        return false;
    }

    @Override
    public void add(int index, String element) {
        if (!contains(element))
            super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        for (String a : c)
            add(a);
        return true;
    }

    @Override
    public double checkCompatibility(ICompatible other) {
        if (!(other instanceof IntentList))
            return 0;

        if (size() == 0) return 1;

        float sum = 0;
        for (String item : this){
            if (((IntentList) other).contains(item))
                sum += 1;
        }
        return sum / size();
    }
}
