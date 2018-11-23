package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;

import java.util.*;

public class IntentClassifier {

    public List<IntentClass> classes = new ArrayList<>();
    //protected static final float SMOOTHING = 0.1F;
    private static final float BORDER = 0.5F;

    public IntentClassifier(List<Phrase> phrases){
        for (Phrase phrase : phrases)
            add(phrase);

        adapt(phrases);
        Main.log("Intent classifier initialization completed");
    }

    public void add(Phrase phrase){
        List<String> words = TextOperations.defaultWordsExtraction(phrase.getPhraseText());

        for (String intent : phrase.getIntents()) {
            boolean found = false;
            for (IntentClass container : classes) {
                if (intent.equals(container.getName())) {
                    found = true;
                    container.add(words);
                    break;
                }
            }
            if (!found) {
                IntentClass container = new IntentClass(intent);
                container.add(words);
                classes.add(container);
            }
        }
    }

    protected Map<String, Float> getProbabilities(String phrase) {
        if (phrase == null) return null;
        return getProbabilities(TextOperations.defaultWordsExtraction(phrase));
    }

    protected Map<String, Float> getProbabilities(List<String> text){
        if (text == null) return null;
        Map<String, Float> probabilities = new HashMap<>();

        for (IntentClass cl : classes){
            float result = 0;
            for (String sWord: text) {
                result += cl.getCount(sWord);
            }
            probabilities.put(cl.getName(), result / text.size());
        }

        return probabilities;
    }

    public IntentList classify(String phrase){
        IntentList result = new IntentList();
        if (phrase == null) return result;

        Map<String, Float> probabilities = getProbabilities(phrase);

        LinkedList<Map.Entry<String, Float>> probabilitiesList = new LinkedList<>(probabilities.entrySet());
        probabilitiesList.sort(new Comparator<Map.Entry<String, Float>>() {
            @Override public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return Float.compare(o2.getValue(), o1.getValue());
            }
        });

        while (probabilities.size() > 0) {
            Map.Entry<String, Float> entry = probabilitiesList.getFirst();
            if (entry.getValue() > BORDER) {
                result.add(entry.getKey());
                probabilitiesList.removeFirst();
            } else break;
        }

        return result;
    }
    public IntentClass getByName(String name){
        for (IntentClass c : classes){
            if (c.getName().equals(name))
                return c;
        }
        return null;
    }

    private static final float ADAPTATION_FORCE = 0.01F;
    private static final int MAX_ROUND = 100;

    protected void adapt(List<Phrase> phrases){
        List<String> correctIntents = new ArrayList<>();
        Map<String, Integer> overallIntentsCount = new HashMap<>();
        for (Phrase phrase : phrases) {
            for (String intent : phrase.getIntents()){
                int a = overallIntentsCount.getOrDefault(intent, 0);
                overallIntentsCount.put(intent, a+1);
            }
        }
        Map<String, Integer> incorrectAnswers = new HashMap<>();
        for (int _i = 0; _i < MAX_ROUND; _i++) {
            incorrectAnswers.clear();
            for (Phrase phrase : phrases) {
                List<String> words = TextOperations.defaultWordsExtraction(phrase.phraseText);
                for (Map.Entry<String, Float> res : getProbabilities(words).entrySet()) {
                    if (correctIntents.contains(res.getKey())) continue;

                    boolean actual = res.getValue() > BORDER;
                    boolean expected = phrase.getIntents().contains(res.getKey());
                    if (actual != expected){
                        String intent = res.getKey();
                        int a = incorrectAnswers.getOrDefault(intent, 0);
                        incorrectAnswers.put(intent, a+1);

                        for (String word : words){
                            getByName(intent).apply(word, expected ? ADAPTATION_FORCE : -ADAPTATION_FORCE);
                        }
                    }
                }
            }
            for (String intent : incorrectAnswers.keySet()) {
                if (incorrectAnswers.get(intent) <= 1 || (float) incorrectAnswers.getOrDefault(intent, 0) / overallIntentsCount.get(intent) < 0.1)
                    correctIntents.add(intent);
            }

//            System.out.println("Correct: "+correctIntents.size() + " of " + base.classes.size());
//            for (Container c : base.classes)
//                c.optimize();
        }
//        for (Map.Entry<String, Integer> num : incorrectAnswers.entrySet()){
//            System.out.println(num.getKey() + " " + (float) num.getValue() / overallIntentsCount.get(num.getKey()));
//        }
//        for (String intent : overallIntentsCount.keySet()) {
//            if (!correctIntents.contains(intent))
//                System.out.println(intent);
//        }
        System.out.println("Adapting completed");
    }

    private static class IntentClass {

        protected String name;
        protected Map<String, Float> words;

        public IntentClass(String name) {
            words = new HashMap<>();
            this.name = name;
        }

        public void apply(String word, float value){
            value += words.getOrDefault(word, 0F);
            if (value < 0) return;
            words.put(word, value);
        }

        public float getCount(String word) {
            return words.getOrDefault(word, 0F);
            //return Math.max(words.getOrDefault(word, 0F), 0F);
        }

        public String getName() {
            return name;
        }

        public void add(List<String> newWords) {
            if (name.equals("MISS_YOU")){
                this.name = this.name;
            }
            float koef = 1.0F / newWords.size();
            for (String word : newWords) {
                Float count = words.getOrDefault(word, 0F);
                words.put(word, count + koef);
            }
        }

    }
}

