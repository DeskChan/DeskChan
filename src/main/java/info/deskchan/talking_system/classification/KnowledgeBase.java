package info.deskchan.talking_system.classification;

import info.deskchan.core_utils.TextOperations;
import info.deskchan.talking_system.Phrase;

import java.util.ArrayList;
import java.util.List;

class KnowledgeBase {

    public List<Container> classes = new ArrayList<>();

    public void add(Phrase phrase) {
        String text = phrase.getPhraseText();
        text = text.replace("\\{[^\\{\\}]+\\}", "");
        ArrayList<String> words = TextOperations.simplifyWords(TextOperations.extractWordsLower(text));

        for (String purpose : phrase.getPurposes()) {
            boolean found = false;
            for (Container container : classes) {
                if (purpose.equals(container.getName())) {
                    found = true;
                    container.add(words);
                    break;
                }
            }
            if (!found) {
                Container container = new Container(purpose);
                container.add(words);
                classes.add(container);
            }
        }
    }

    public float countClassProbability(Container container){
        if (classes.size() == 0)
            return 0.0F;

        int allWords = 0;
        for (Container cl : classes)
            allWords += cl.getUniqueWordsCount();

        return (float) container.getUniqueWordsCount() / allWords;
    }

}