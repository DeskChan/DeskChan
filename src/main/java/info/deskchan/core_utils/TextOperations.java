package info.deskchan.core_utils;

import java.util.*;


public class TextOperations {
    private final static char[][] simplify = {{'ъ', 'й'}, {'ь', 'й'}, {'ы', 'и'}, {'ё', 'е'}};
    private final static String VOWELS = "уеъыаоэяиью";

    public static ArrayList<String> simplifyWords(String[] words) {
        ArrayList<String> w = new ArrayList<>();
        String cw;
        for (int i = 0; i < words.length; i++) {
            cw = simplifyWord(words[i]);
            if (cw.equals("не") && i < words.length - 1) {
                String cw2 = simplifyWord(words[i + 1]);
                if (!cw2.equals("не")) {
                    w.add(cw + cw2);
                    i++;
                    continue;
                }
            }
            w.add(cw);
        }
        return w;
    }

    public static ArrayList<String> simplifyWords(ArrayList<String> words) {
        ArrayList<String> w = new ArrayList<>();
        String cw;
        for (int i = 0; i < words.size(); i++) {
            cw = simplifyWord(words.get(i));
            if (cw.equals("не") && i < words.size() - 1) {
                String cw2 = simplifyWord(words.get(i + 1));
                if (!cw2.equals("не")) {
                    w.add(cw + cw2);
                    i++;
                    continue;
                }
            }
            w.add(cw);
        }
        return w;
    }

    public static String simplifyWord(String word) {
        StringBuilder sb = new StringBuilder(word);
        for (int i = 0; i < sb.length(); i++) {
            if (i > 0 && sb.charAt(i) == sb.charAt(i - 1)) {
                sb.deleteCharAt(i);
                i--;
                continue;
            }
            for (int k = 0; k < simplify.length; k++)
                if (sb.charAt(i) == simplify[k][0]) {
                    sb.setCharAt(i, simplify[k][1]);
                    break;
                }
        }

        if (sb.length() > 0 && notOfVowels(sb.toString()))
            while (VOWELS.contains(sb.subSequence(sb.length()-1, sb.length())))
                sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    private enum WordExtractionMode { WORDS_NO_CHANGE, WORDS_LOWER, WORDS_UPPER, IMPORTANT_PARTS_LOWER }

    public static ArrayList<String> extractWords(String phrase) {
        return extractWordsImpl(phrase, WordExtractionMode.WORDS_NO_CHANGE);
    }

    public static ArrayList<String> extractWordsLower(String phrase) {
        return extractWordsImpl(phrase, WordExtractionMode.WORDS_LOWER);
    }

    public static ArrayList<String> extractWordsUpper(String phrase) {
        return extractWordsImpl(phrase, WordExtractionMode.WORDS_UPPER);
    }

    public static ArrayList<String> extractSpeechParts(String phrase) {
        return extractWordsImpl(phrase, WordExtractionMode.IMPORTANT_PARTS_LOWER);
    }

    private static ArrayList<String> extractWordsImpl(String phrase, WordExtractionMode mode) {
        ArrayList<String> words = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= phrase.length(); i++) {
            char at = i<phrase.length() ? phrase.charAt(i) : '\0';
            if (i == phrase.length() || at == ' ' || at == '\n') {
                if (sb.length() == 0) continue;

                words.add(sb.toString());
                sb = new StringBuilder();

            } else if (isLetter(at)) {
                switch (mode) {
                    case WORDS_NO_CHANGE:
                        sb.append(at);
                        break;
                    case WORDS_LOWER: case IMPORTANT_PARTS_LOWER:
                        sb.append(Character.toLowerCase(at));
                        break;
                    case WORDS_UPPER:
                        sb.append(Character.toUpperCase(at));
                        break;
                }
            } else if (mode == WordExtractionMode.IMPORTANT_PARTS_LOWER){
                if (at == '?' || at == '!' || at == '.'){
                    if (i == 0 || isLetter(phrase.charAt(i-1))){
                        if (sb.length() == 0) continue;
                        words.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    sb.append(at);
                }
            }
        }
        return words;
    }

    private static boolean isLetter(char c){
        return Character.isLetter(c) || Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CYRILLIC);
    }

    private static boolean notOfVowels(String text){
        for (int i = 0; i < text.length(); i++)
            if (!VOWELS.contains(text.substring(i,i+1))) return true;
        return false;
    }
}