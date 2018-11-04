package info.deskchan.talking_system;

import info.deskchan.talking_system.intent_exchange.ICompatible;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentList extends ArrayList<String> implements ICompatible {

    public IntentList(){ super(); }

    public IntentList(Collection<String> intents){
        super();
        if (intents != null){
            addAll(intents);
        }
    }

    public IntentList(String... intents){
        super();
        for (String item : intents) add(item);
    }

    public IntentList(String text){
        this(text.split("[\\s,]+"));
    }

    @Override
    public boolean add(String element) {
        element = format(element);
        if (element.length() == 0)
            return false;
        if (!contains(element))
            return super.add(element);
        return false;
    }

    @Override
    public void add(int index, String element) {
        element = format(element);
        if (element.length() == 0)
            return;
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        if (c == null) return false;
        for (String a : c)
            add(a);
        return true;
    }

    @Override
    public boolean contains(Object o) {
        return super.contains(format(o.toString()));
    }

    private String format(String a){
        return a.trim().toUpperCase();
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

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = iterator();
        if (it.hasNext()){
            sb.append(it.next());
            while (it.hasNext()){
                sb.append(", "+it.next());
            }
        }
        return sb.toString();
    }
}
