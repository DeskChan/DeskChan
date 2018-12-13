package info.deskchan.talking_system;

import info.deskchan.core.Path;
import info.deskchan.core_utils.TextOperations;
import info.deskchan.talking_system.neural_classifier.DataSet;
import info.deskchan.talking_system.neural_classifier.NeuralNetwork;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeuralCharacterClassifier implements CharacterClassifier {

    Map<String, Integer> overAllDictionary = new HashMap<>();
    List<String> inputWords;

    static boolean debug = true;

    NeuralNetwork classifier;

    public NeuralCharacterClassifier(Path path) throws Exception {

        classifier = new NeuralNetwork(path);
        inputWords = new ArrayList<>();
        for (int i = 0; i < classifier.getInputLayerSize(); i++)
            inputWords.add(classifier.getInputNeuronName(i));
    }

    public NeuralCharacterClassifier(List<Phrase> phrases, Path save) {
        new Thread( () -> {
            Main.log("Generating new character classification model");
            List<TransformedPhrase> transformedPhrases = new LinkedList<>();

            for (Phrase phrase : phrases) {
                transformedPhrases.add(new TransformedPhrase(phrase));
            }

            inputWords = new ArrayList<>();
            inputWords.add("length");
            for (String key : overAllDictionary.keySet()) {
                //if (overAllDictionary.get(key) > 1)
                inputWords.add(key);
            }

            int inputsSize = inputWords.size();

            int outs = 16;
            NeuralNetwork net = new NeuralNetwork(inputsSize, outs * 2, outs);
            classifier = net;

            net.learningRate = Main.getProperties().getFloat("cc-model-learning-rate", 0.05);

            net.setInputNeuronsMapping(inputWords);

            DataSet trainingSet = new DataSet(net);
            for (TransformedPhrase phrase : transformedPhrases) {
                trainingSet.addRow(phrase.getData(inputWords), phrase.diapasons);
            }

            double minError = 0.05;
            int cycle = Main.getProperties().getInteger("cc-model-rounds", 100);
            net.useDropping = false;
            net.storeBest = true;

            example = transformedPhrases.get(59);
            while (net.getCurrentError() > minError && cycle > 0) {
                net.learn(trainingSet, 5, minError);
                cycle--;

                if (debug) {
                    debugPrint();
                }
            }

            try {
                net.saveToFile(save);
            } catch (Exception e) {
                Main.log(e);
            }
            Main.log("New character classification model generation completed");
        }).start();

    }

    public void add(Phrase phrase){
        TransformedPhrase tp = new TransformedPhrase(phrase);
        DataSet trainingSet = new DataSet(classifier);
        trainingSet.addRow(tp.getData(inputWords), tp.diapasons);

        classifier.learn(trainingSet, 50, 0);
    }

    TransformedPhrase example;

    public void debugPrint(){
        for (Map.Entry<String, Double> data : example.getData(inputWords).entrySet())
            classifier.setInput(data.getKey(), data.getValue());
        classifier.activate();

        double[] output = classifier.getOutput();
        for (int i = 0; i < 8; i++) {
            double outCenter = output[i * 2 + 1], expectedCenter = example.diapasons[i * 2 + 1];
            double outRadius = output[i * 2], expectedRadius = example.diapasons[i * 2];

            outCenter = (outCenter - 0.5) * 8;
            outRadius = outRadius * 4;
            expectedCenter = (expectedCenter - 0.5) * 8;
            expectedRadius = expectedRadius * 4;

            int a = (int) Math.round(Math.max(outCenter - outRadius, -4)), b = (int) Math.round(Math.min(outCenter + outRadius, 4));
            int c = (int) Math.round(Math.max(expectedCenter - expectedRadius, -4)), d = (int) Math.round(Math.min(expectedCenter + expectedRadius, 4));
            System.out.println(output[i * 2] + " " + output[i * 2 + 1] + " " + a + " / " + b + " --- " + c + " / " + d);
        }
    }

    public CharacterRange getCharacterForPhrase(String text){
        TransformedPhrase phrase = new TransformedPhrase(text, null);
        classifier.setInputs(phrase.getData(inputWords));
        classifier.activate();

        CharacterRange range = new CharacterRange();
        double[] output = classifier.getOutput();
        for (int i = 0; i < classifier.getOutputLayerSize() / 2; i++) {
            double outCenter = output[i * 2 + 1];
            double outRadius = output[i * 2];

            outCenter = (outCenter - 0.5) * 8;
            outRadius = outRadius * 4;

            int a = (int) Math.round(Math.max(outCenter - outRadius, -4)), b = (int) Math.round(Math.min(outCenter + outRadius, 4));
            range.range[i] = new Range(a, b);
        }
        return range;
    }

    static Pattern pattern = Pattern.compile("[\\?\\!\\.]+");
    class TransformedPhrase {

        double[] diapasons = new double[16];
        String text;
        HashMap<String, Integer> dictionary = new HashMap<>();
        public TransformedPhrase(Phrase phrase){
            text = phrase.phraseText;
            List<String> words = TextOperations.simplifyWords(TextOperations.extractSpeechParts(phrase.phraseText));
            if (!phrase.spriteType.equals(Phrase.DEFAULT_SPRITE))
                words.add(phrase.spriteType);
            Matcher m = pattern.matcher(phrase.phraseText);
            while (m.find()) {
                words.add(m.group());
            }

            for (String word : words){
                dictionary.put(word, dictionary.getOrDefault(word, 0) + 1);
                overAllDictionary.put(word, overAllDictionary.getOrDefault(word, 0) + 1);
            }
            for (int i = 0; i < 8; i++) {
                Range range = phrase.character.range[i];
                diapasons[i*2] = (range.end - range.start) / 8.0;
                diapasons[i*2 + 1] = 0.5 + (range.end + range.start) / 16.0;
                //System.out.println(phrase.character.range[i].start + " / " + phrase.character.range[i].end + " // " + diapasons[i*2] + " " + diapasons[i*2 + 1]);
            }
           // System.out.println();
        }

        public TransformedPhrase(String text, String sprite){
            this.text = text;
            List<String> words = TextOperations.simplifyWords(TextOperations.extractSpeechParts(text));
            if (sprite != null)
                words.add(sprite);
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                words.add(m.group());
            }

            for (String word : words){
                dictionary.put(word, dictionary.getOrDefault(word, 0) + 1);
            }
        }

        public Map<String, Double> getData(List<String> keys){
            Map<String, Double> data = new HashMap<>();
            double len = (double) dictionary.keySet().size() - 1;
            len = Math.min(len, 20) / 20;

            for (String key : keys)
                data.put(key, dictionary.containsKey(key) ? 1.0 : 0.0);

            data.put("length", len);

            return data;
        }
    }
}
