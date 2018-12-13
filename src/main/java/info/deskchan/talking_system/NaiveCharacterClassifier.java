package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;

import java.util.*;

public class NaiveCharacterClassifier implements CharacterClassifier {

    public RangeClass[] classes;

    public NaiveCharacterClassifier(List<Phrase> phrases){

        //phrases = phrases.subList(62, 63);

        classes = new RangeClass[CharacterRange.getFeatureCount() * 2];
        for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
            classes[i * 2] = new RangeClass(0);
            classes[i * 2 + 1] = new RangeClass(CharacterRange.BORDER);
        }

        for (Phrase phrase : phrases)
            add(phrase);

        for (RangeClass r : classes)
            r.apply();

        //System.out.println(phrases.get(62).character);
        //System.out.println(getCharacterForPhrase(phrases.get(62).phraseText));
        //adapt(phrases);
        //System.out.println(getCharacterForPhrase(phrases.get(62).phraseText));
        Main.log("Character classifier initialization completed");
    }

    public void add(Phrase phrase){
        List<String> words = getWordsFromPhrase(phrase.getPhraseText());

        for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
            Range r = phrase.character.range[i];
            classes[i * 2].add(words, r.getCenter(), 1);
            classes[i * 2 + 1].add(words, r.getRadius(), 1);
        }
    }

    protected List<String> getWordsFromPhrase(String text){
        text = text.replace("\\{[^\\{\\}]+\\}", "");
        return TextOperations.simplifyWords(TextOperations.extractSpeechParts(text));
    }

    public CharacterRange getCharacterForPhrase(String text) {
        return getCharacterForPhrase(getWordsFromPhrase(text));
    }


    public CharacterRange getCharacterForPhrase(List<String> words){
        CharacterRange result = new CharacterRange();

        double[] ranges = getRanges(words);

        for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
            double center = ranges[i*2], radius = ranges[i*2+1];
            int a = (int) Math.round(center - radius), b = (int) Math.round(center + radius);
            a = Math.max(-CharacterRange.BORDER, a);
            b = Math.min( CharacterRange.BORDER, b);
            result.range[i] = new Range(a, b);
        }
        return result;
    }

    private double[] getRanges(List<String> words){
        double[] result = new double[CharacterRange.getFeatureCount() * 2];

        for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
            float center = 0, radius = 0, cc = 0, rc = 0;

            for (String word : words) {
                RangeClass.Value val = classes[i * 2].getValue(word);
                center += val.getValue() * val.getWeight();
                cc += val.getWeight();

                val = classes[i * 2 + 1].getValue(word);
                radius += val.getValue() * val.getWeight();
                rc += val.getWeight();
            }

            center /= cc;
            radius /= rc;
            result[i*2] = center;
            result[i*2+1] = radius;
        }
        return result;
    }

    private static final float ADAPTATION_FORCE = 0.1F;
    private static final int MAX_ROUND = 100;

    protected void adapt(List<Phrase> phrases){
        double bestError = Double.MAX_VALUE;

        for (int _i = 0; _i < MAX_ROUND; _i++) {
            double maxDeviation = 0,
                   middleDeviation = 0, overall = 0;
            int mc = 0;
            for (Phrase phrase : phrases) {
                List<String> words = getWordsFromPhrase(phrase.phraseText);
                double[] actual = getRanges(words);
                CharacterRange expected = phrase.character;

                for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
                    double a = Math.abs(actual[i*2] - expected.range[i].getCenter()),
                           b = Math.abs(actual[i*2+1] - expected.range[i].getRadius());
                    middleDeviation += a + b;
                    mc += 2;
                    maxDeviation = Math.max(Math.max(a,b), maxDeviation);
                    overall += a + b;

                    classes[i * 2].    add(words, expected.range[i].getCenter(), (float) a * ADAPTATION_FORCE);
                    classes[i * 2 + 1].add(words, expected.range[i].getRadius(), (float) b * ADAPTATION_FORCE);
                }
            }

            for (RangeClass r : classes)
                r.apply();

            middleDeviation /= mc;
            bestError = Math.min(overall, bestError);
            System.out.println("Overall: " + overall +
                    " / Middle deviation: " + middleDeviation +
                    " / Max deviation: " + maxDeviation +
                    " / Best error: " + bestError);
        }
        //for (String intent : overallIntentsCount.keySet()) {
        //    if (!correctIntents.contains(intent))
        //        System.out.println(intent);
        //}
        System.out.println("Adapting completed");
    }
}

class RangeClass {

    protected Map<String, Value> words;
    protected float START_VALUE = 0;

    public RangeClass(float startValue) {
        words = new HashMap<>();
        START_VALUE = startValue;
    }

    public void apply(){
        //for (Value val : words.values()){
        for (Map.Entry<String, Value> val : words.entrySet()){
            val.getValue().applyBuffer();
        }
    }

    public Value getValue(String word) {
        return words.getOrDefault(word, new Value());
    }

    public void add(List<String> newWords, float value, float weight) {
        if (weight == 0) return;
        float koef = weight / newWords.size();
        for (String word : newWords) {
            Value val = words.getOrDefault(word, new Value());
            val.add(value, koef);
            words.put(word, val);
        }
    }

    protected class Value {

        private class Pair {
            public float value = START_VALUE, weight = 0;
            Pair(float v, float w){ value = v; weight = w; }
            Pair(){}
        }

        private Pair value = new Pair();
        private List<Pair> buffer = new LinkedList<>();

        public void add(float value, float weight){
            buffer.add(new Pair(value, weight));
        }

        public void applyBuffer(){
            if (buffer.size() == 0) return;
            float v = 0, w = 0;
            for (Pair pair : buffer){
                v += pair.value * pair.weight;
                w += pair.weight;
            }
            value.value = (value.value * value.weight + v) / (value.weight + w);
            value.weight = w;
            buffer.clear();
        }

        public float getValue(){ return value.value; }
        public float getWeight(){ return value.weight; }

        @Override
        public String toString() {
            return "{"+value.value+";"+value.weight+"}";
        }
    }

}