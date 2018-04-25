package info.deskchan.talking_system.classification;

import java.util.HashMap;
import java.util.List;

class Container {
    protected String name;
    //protected HashMap<String, Integer> words;
    private float sum;
    protected HashMap<String, Float> words;

    public Container(String name) {
        this.name = name;
        words = new HashMap<>();
    }

    public void add(List<String> newWords) {
        float koef = 1.0F / newWords.size();
        //System.out.println(newWords + " " + koef);
        for (String word : newWords) {
            Float count = words.getOrDefault(word, 0F);
            words.put(word, count + koef);
        }
        sum += 1;
    }

    public String getName() {
        return name;
    }

    //public int getCount(String word) {
    //    return words.getOrDefault(word, 0);
    //}

    public float getCount(String word) {
        return words.getOrDefault(word, 0F);
    }

    public int getUniqueWordsCount() {
        return words.keySet().size();
    }

    public float getSum() {
        return sum;
    }
}