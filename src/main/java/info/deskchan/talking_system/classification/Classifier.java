package info.deskchan.talking_system.classification;

import info.deskchan.core_utils.TextOperations;
import info.deskchan.talking_system.Phrase;

import java.util.*;

public class Classifier {

    public KnowledgeBase base;
    protected static final float BORDER = 0.01F;
    //protected static final float SMOOTHING = 0.1F;

    public Classifier() {
        base = new KnowledgeBase();
    }

    public void add(Phrase phrase){
        base.add(phrase);
    }

    public List<String> classify(String phrase){
        ArrayList<String> text = TextOperations.simplifyWords(TextOperations.extractWordsLower(phrase));
        Map<String, Float> probabilities = new HashMap<>();

        for (Container cl : base.classes){
            /*float classProbability = base.countClassProbability(cl);

            float multiply = 1;
            for (String sWord: text) {
                multiply *= (cl.getCount(sWord) + SMOOTHING) / (classProbability + cl.getUniqueWordsCount() * SMOOTHING);
                System.out.println(cl.getName() + " " + sWord + " " + cl.getCount(sWord) + " " + (classProbability + cl.getUniqueWordsCount() * 0.7) + " " + multiply);
            }
            System.out.println(cl.getName() + " " + multiply * classProbability);
            probabilities.put(cl.getName(), multiply * classProbability);*/

            float result = 0;
            for (String sWord: text) {
                result += cl.getCount(sWord);
            }
            System.out.println(cl.getName() + " " + result / cl.getSum());
            probabilities.put(cl.getName(), result / cl.getSum());
        }

        LinkedList<Map.Entry<String, Float>> probabilitiesList = new LinkedList<>(probabilities.entrySet());
        probabilitiesList.sort(new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return Float.compare(o2.getValue(), o1.getValue());
            }
        });

        List<String> result = new ArrayList<>();
        while (probabilitiesList.size() > 0) {
            Map.Entry<String, Float> entry = probabilitiesList.getFirst();
            if (entry.getValue() > BORDER) {
                result.add(entry.getKey());
                probabilitiesList.removeFirst();
            } else break;
        }

        return result;
    }
}
