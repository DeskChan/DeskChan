package info.deskchan.chat_window;

import info.deskchan.core_utils.TextOperations;

import java.util.List;

public class FixLayout {
    private static char[] englishConsonants="wrtp[]sdfghjkl;'zxcvbnm,.".toCharArray();
    private static char[] russianConsonants="йцкнгшщзхъфвпрлджчсмтб".toCharArray();

    private static char[] englishLayout="qwertyuiop[]asdfghjkl;'zxcvbnm,./QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?".toCharArray();
    private static char[] russianLayout="йцукенгшщзхъфывапролджэячсмитьбю.ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,".toCharArray();

    public static String fixRussianEnglish(String text){
        float a=isLayoutMissedRussian(text);
        float b=isLayoutMissedEnglish(text);
        float c=Math.max(a,b);
        if(c>0.5){
            String tr=translate(text,englishLayout,russianLayout);
            a=isLayoutMissedRussian(tr);
            b=isLayoutMissedEnglish(tr);
            if(c>Math.max(a,b)) text=tr;
        }
        return text;
    }
    public static float isLayoutMissedRussian(String text){
        return isLayoutMissed(text,russianConsonants);
    }
    public static float isLayoutMissedEnglish(String text){
        return isLayoutMissed(text,englishConsonants);
    }
    private static String translate(String text,char[] from, char[] to){
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
