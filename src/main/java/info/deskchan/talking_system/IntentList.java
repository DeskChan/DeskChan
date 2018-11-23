package info.deskchan.talking_system;

import java.util.*;

public class IntentList extends ArrayList<String> {

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
        if (element == null) return false;
        element = format(element);
        if (element.length() == 0)
            return false;
        if (!contains(element))
            return super.add(element);
        return false;
    }

    @Override
    public void add(int index, String element) {
        if (element == null){
            remove(index);
            return;
        }
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
    public boolean equals(Object other){
        if (other == null) return false;
        if (other instanceof Collection){
            Collection<Object> compare = (Collection<Object>) other;
            if (compare.size() != size()) return false;
            for (Object c : compare){
                if (c == null || !contains(c.toString())) return false;
            }
            return true;
        } else if (other instanceof String){
            return equals(new IntentList(other.toString()));
        }
        return false;
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
