package info.deskchan.speech_command_system;

import info.deskchan.core_utils.LimitHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhraseComparison {

    private final static Map<String, String> replaceableToLatin = new HashMap<String, String>(){{
        String a = "а a б b в v г g д d е e ё yo ж zh з z и i й y к k л l м m н n о o п p р r с s т t у u ф f х h ц ts ч ch ш sh щ sh ъ _ ы i ь _ э e ю yu я ya";

        String[] ar = a.split(" ");
        for(int i = 0; i < ar.length; i += 2)
            put(ar[i], ar[i+1].equals("_") ? "" : ar[i+1]);
    }};

    private final static Map<String, String> replaceableSymplifying = new HashMap<String, String>(){{
        String a = "yo a ts c yu u ya a d t j sh g k q k v f w f y i shch sh ee i oo u ae a ea a ai a tion shn z s ts c zh sh e i o a";

        String[] ar = a.split(" ");
        for(int i = 0; i < ar.length; i += 2)
            put(ar[i], ar[i+1].equals("_") ? "" : ar[i+1]);
    }};

    public final static float ACCURACY = 0.65f;

    private final static String suffixes1 = "aeiouys", suffixes2 = "gmthg";
    private static LimitHashMap<String, String> suffixCache = new LimitHashMap<>(500);

    public static String removeSuffix(String word){
        int i, len = word.length()-1;
        boolean hard = false;
        for(i=0; i<3 && len-i >= 0; i++){
            char c = word.charAt(len-i);
            if(suffixes1.indexOf(c)<0) break;
            else if(i<2 || suffixes2.indexOf(c)>=0){
                if(hard) break;
                else hard = true;
            }
        }
        return len-i>1 ? word.substring(0, len+1-i) : word;
    }

    private static String simplify(String word){
        String repl = suffixCache.get(word);
        if(repl != null) return repl;

        repl = word;
        StringBuilder sb = new StringBuilder(word);

        int pos;
        for(Map.Entry<String, String> replace : replaceableSymplifying.entrySet()){
            pos = 0;
            while((pos = sb.indexOf(replace.getKey(), pos)) >= 0) {
                sb.replace(pos, pos + replace.getKey().length(), replace.getValue());
                pos += replace.getValue().length();
            }
        }

        for(int i=1; i<sb.length(); i++)
            if(sb.charAt(i) == sb.charAt(i-1)){
                sb.deleteCharAt(i);
                i--;
            }

        repl = sb.toString();
        suffixCache.put(word, repl);
        return repl;
    }

    private static class WordPair{
        String one, two;
        WordPair(String o, String t) {
            one = toLatin(o); two = toLatin(t);
        }
        @Override
        public boolean equals(Object other){
            return other instanceof WordPair && one.equals(((WordPair) other).one) && two.equals(((WordPair) other).two);
        }
        private String toLatin(String word){
            int pos;
            StringBuilder sb = new StringBuilder(word);
            for(Map.Entry<String, String> replace : replaceableToLatin.entrySet()){
                pos = 0;
                while((pos = sb.indexOf(replace.getKey(), pos)) >= 0) {
                    sb.replace(pos, pos + replace.getKey().length(), replace.getValue());
                    pos += replace.getValue().length();
                }
            }
            return sb.toString();
        }
        WordPair simplified(){
            return new WordPair(simplify(one), simplify(two));
        }
        WordPair withoutSuffixes(){
            return new WordPair(removeSuffix(one), removeSuffix(two));
        }
        @Override
        public String toString(){
            return "["+one+", "+two+"]";
        }
    }
    private static LimitHashMap<WordPair, Float> compareCache = new LimitHashMap<>(500);

    public static int borderedAbsolute(String one, String two, int border){
        if(one.length() > border) one = one.substring(0, border);
        if(two.length() > border) two = two.substring(0, border);
        return (int) complexLevenshtein(new WordPair(one, two), false);
    }
    public static int borderedAbsolute(String one, String two){
        int border = Math.min(one.length(),two.length());
        return (int) complexLevenshtein(new WordPair(one.substring(0, border), two.substring(0, border)), false);
    }
    public static int absolute(String one, String two){
        return (int) complexLevenshtein(new WordPair(one, two), true);
    }
    public static float relative(String one,String two){
        int lensum = one.length()+two.length();
        return 1 - (complexLevenshtein(new WordPair(one, two), true) * 2 / lensum);// -
            //    Math.abs(one.length()-two.length()) / 30.0f;
    }
    private static float complexLevenshtein(WordPair pair, boolean removeSuffixes){
        if(pair.one.equals(pair.two)) return 0;
        Float repl = compareCache.get(pair);
        if(repl != null) return repl;

        int l1 = Levenshtein(pair);
        int l2 = Levenshtein(removeSuffixes ? pair.simplified().withoutSuffixes() : pair.simplified());

        float l = (l1+l2)/2.0f;
        compareCache.put(pair, l);
        return l;
    }
    private static int Levenshtein(WordPair pair){
        StringBuilder sb1 = new StringBuilder(pair.one),
                      sb2 = new StringBuilder(pair.two);
        int L1 = sb1.length()+1, L2 = sb2.length()+1;
        int[][] mat = new int[L1][L2];

        for(int i=0; i<L1; i++) mat[i][0]=i;
        for(int j=0; j<L2; j++){
            mat[0][j]=j;
            for(int i=1; i<L1 && j>0; i++){
                char o=(i-1 < sb1.length() ? sb1.charAt(i-1) : ' ');
                mat[i][j]=Math.min(
                        Math.min(mat[i][j-1] , mat[i-1][j]) + 1 ,
                        mat[i-1][j-1] + (o==sb2.charAt(j-1) ? 0 : 1)
                );
                if(i>1 && j>1)
                    if(o==sb2.charAt(j-1) && o==sb2.charAt(j-2))
                        mat[i][j]=Math.min( mat[i][j] , mat[i-2][j-2]+1 );
            }
        }
        return mat[L1-1][L2-1];
    }
    public static ArrayList<String> toClearWords(String text){
        ArrayList<String> str_list=new ArrayList<String>();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<text.length();i++){
            if(Character.isLetterOrDigit(text.charAt(i)) ||
                    (!Character.isWhitespace(text.charAt(i)) && i>0 && Character.isDigit(text.charAt(i-1)) && i<text.length()-1 && Character.isDigit(text.charAt(i+1)))){
                sb.append(Character.toLowerCase(text.charAt(i)));
            } else if(sb.length()>0){
                str_list.add(sb.toString());
                sb=new StringBuilder();
            }
        }
        if(sb.length()>0)
            str_list.add(sb.toString());
        return str_list;
    }
    private final static Pattern pattern = Pattern.compile("[А-яA-z0-9\\-']+|(\\{\\s*[A-z0-9]+\\s*:\\s*[A-z]+\\s*\\})");
    public static ArrayList<String> toRuleWords(String text){
        ArrayList<String> str_list=new ArrayList<String>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            str_list.add(matcher.group().replace(" ",""));
        }
        return str_list;
    }
}