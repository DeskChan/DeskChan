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
        if (size()>=limit)
        {
            K removeKey = orderedKeySet.remove(0);
            this.remove(removeKey);
        }
        if(!containsKey(key)){
            orderedKeySet.add(key);
        }
        return super.put(key, value);
    }
}
