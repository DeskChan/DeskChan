package info.deskchan.chat_window;

import info.deskchan.core_utils.TextOperations;

import java.util.List;

/** Layout fixing class. Use fix* function to automatically fix it. Use isLayoutMissed* to check, is layout missed.  **/
public class FixLayout {
    private static final char[] ENGLISH_CONSONANTS ="wrtp[]sdfghjkl;'zxcvbnm,.".toCharArray();
    private static final char[] RUSSIAN_CONSONANTS ="йцкнгшщзхъфвпрлджчсмтб".toCharArray();

    private static final char[] ENGLISH_LAYOUT ="qwertyuiop[]asdfghjkl;'zxcvbnm,./QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?".toCharArray();
    private static final char[] RUSSIAN_LAYOUT ="йцукенгшщзхъфывапролджэячсмитьбю.ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,".toCharArray();

    /** Automatically find wrong layout and fix it. **/
    public static String fixRussianEnglish(String text){
        float a = isLayoutMissedRussian(text);
        float b = isLayoutMissedEnglish(text);
        float c = Math.max(a,b);

        // we know, that layout missed, so we change it
        if(c>=0.5){
            String tr = translate(text, ENGLISH_LAYOUT, RUSSIAN_LAYOUT);
            a = isLayoutMissedRussian(tr);
            b = isLayoutMissedEnglish(tr);
            if(c > Math.max(a, b)) text=tr;
        }
        return text;
    }

    public static float isLayoutMissedRussian(String text){
        return isLayoutMissed(text, RUSSIAN_CONSONANTS);
    }
    public static float isLayoutMissedEnglish(String text){
        return isLayoutMissed(text, ENGLISH_CONSONANTS);
    }
    private static String translate(String text, char[] from, char[] to){
        StringBuilder textCopy=new StringBuilder(text);
        for(int i=0;i<text.length();i++){
            boolean found = false;
            for(int k=0;k<from.length;k++){
                if(text.charAt(i)==from[k]){
                    textCopy.setCharAt(i,to[k]);
                    found = true;
                    break;
                }
            }
            if(found) continue;
            for(int k=0;k<to.length;k++){
                if(text.charAt(i)==to[k]){
                    textCopy.setCharAt(i,from[k]);
                    break;
                }
            }
        }
        return textCopy.toString();
    }
    private static float isLayoutMissed(String text,char[] consonants){
        float sum=0;
        List<String> words = TextOperations.extractWordsLower(text);
        if(words.size()==0) return 0;
        for(String word : words){
            float maxSequence=0;
            float sequence=0;
            for(int i=0;i<word.length();i++){
                boolean isConsonant=false;
                for(int k=0;k<consonants.length;k++){
                    if(consonants[k]==word.charAt(i)){
                        isConsonant=true;
                        break;
                    }
                }
                if(isConsonant) sequence++;
                else {
                    if(maxSequence<sequence)
                        maxSequence=sequence;
                    sequence=0;
                }
            }
            sum+=Math.max(sequence,maxSequence)/word.length();
        }
        return sum/words.size();
    }
}
