package info.deskchan.talking_system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class TextOperations {
    private final static char[][] simplify={{'ъ','й'},{'ь','й'},{'ы','и'},{'ё','е'}};
    public static String[] simplifyWords(String[] words){
        ArrayList<String> w=new ArrayList<>();
        String cw;
        for(int i=0;i<words.length;i++){
            cw=simplifyWord(words[i]);
            if(cw.equals("не") && i<words.length-1){
                String cw2=simplifyWord(words[i+1]);
                if(!cw2.equals("не")){
                    w.add(cw+cw2);
                    i++;
                    continue;
                }
            }
            w.add(cw);
        }
        return w.toArray(new String[w.size()]);
    }
    public static String simplifyWord(String word){
        StringBuilder sb=new StringBuilder(word);
        for(int i=0;i<sb.length();i++)
            for(int k=0;k<simplify.length;k++)
                if(sb.charAt(i)==simplify[k][0]){
                    sb.setCharAt(i,simplify[k][1]);
                    break;
                }
        return sb.toString();
    }
    public static String[] extractWords(String phrase){
        return extractWords(phrase,0);
    }
    public static String[] extractWordsLower(String phrase){
        return extractWords(phrase,1);
    }
    public static String[] extractWordsUpper(String phrase){
        return extractWords(phrase,2);
    }
    private static String[] extractWords(String phrase,int type){
        LinkedList<String> words=new LinkedList<>();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<phrase.length();i++){
            if(phrase.charAt(i)==' ' || phrase.charAt(i)=='\n'){
                if(sb.length()==0) continue;
                words.add(sb.toString());
                sb=new StringBuilder();
            }
            if(Character.UnicodeBlock.of(phrase.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
                switch(type){
                    case 0: sb.append(phrase.charAt(i)); break;
                    case 1: sb.append(Character.toLowerCase(phrase.charAt(i))); break;
                    case 2: sb.append(Character.toUpperCase(phrase.charAt(i))); break;
                }
            }
        }
        return words.toArray(new String[words.size()]);
    }
}
