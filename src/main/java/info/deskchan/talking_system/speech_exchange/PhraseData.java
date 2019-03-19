package info.deskchan.talking_system.speech_exchange;

import info.deskchan.core_utils.TextOperations;

import java.util.List;

public class PhraseData implements IExchangeable {

    /** Phrase text */
    final String text;

    /** Cached words from phrase */
    private final List<String> words;

    public PhraseData(String text){
        this.text = text;
        words = TextOperations.extractWordsLower(text);
    }

    @Override
    public double checkCompatibility(ICompatible other){
        if (!(other instanceof PhraseData)) return 0;

        PhraseData o = (PhraseData) other;
        return TextOperations.phraseSimpleMatch(this.words, o.words) ? 1 : 0;
    }

    @Override
    public String toString(){
        return text;
    }

    @Override
    public boolean equals(Object other){
        if (!(other instanceof PhraseData)) return false;
        return checkCompatibility((PhraseData) other) == 1;
    }
}
