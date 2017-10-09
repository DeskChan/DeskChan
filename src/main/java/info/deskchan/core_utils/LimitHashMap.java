package info.deskchan.core_utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LimitHashMap<K,V> extends HashMap<K,V> {
    private List<K> orderedKeySet = new ArrayList<>();
    private int limit;
    public LimitHashMap(int maxCapacity){
        super();
        limit = maxCapacity;
    }
    public V put(K key, V value){
        if(!containsKey(key))
            while(orderedKeySet.size()>=limit){
                remove(orderedKeySet.get(0));
                orderedKeySet.remove(0);
            }

        return super.put(key, value);
    }
}
