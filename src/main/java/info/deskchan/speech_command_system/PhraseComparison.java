package info.deskchan.speech_command_system;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhraseComparison {
    private static String[][] replaceable=new String[][]{
            {"о","а"} , {"е","и"} , {"д","т"} , {"г","к"} , {"ж","ш"} , {"ы","и"} , {"з","с"} , {"б","п"} , {"в","ф"}, {"ь",""}, {"ъ",""}, {"тс","ц"}, {"тщ","ч"}
    };
    private static String suffixes = "аеюиэйуъыояью";
    public static String removeSuffix(String word){
        int i=0, len = word.length()-1;
        for(i=0; i<3; i++)
            if(suffixes.indexOf(word.charAt(len-i))<0) break;
        return len+1-i>1 ? word.substring(0, len+1-i) : word;
    }
    public static int borderedAbsolute(String one, String two, int border){
        int L=4;
        if(one.length()>L) one = one.substring(0, L);
        if(two.length()>L) two = two.substring(0, L);
        for(int k=0;k<replaceable.length;k++){
            one = one.replaceAll(replaceable[k][0], replaceable[k][1]);
            two = two.replaceAll(replaceable[k][0], replaceable[k][1]);
        }
        return Levenshtein(one, two);
    }
    public static int borderedAbsolute(String one, String two){
        int L=Math.min(one.length(),two.length());
        if(one.substring(0,L).equals(two.substring(0,L))) return 0;
        for(int k=0;k<replaceable.length;k++){
            one = one.replaceAll(replaceable[k][0], replaceable[k][1]);
            two = two.replaceAll(replaceable[k][0], replaceable[k][1]);
        }
        L=Math.min(one.length(),two.length());
        return Levenshtein(one.substring(0, L), two.substring(0, L));
    }
    public static int absolute(String one, String two){
        if(one.equals(two)) return 0;
        one = removeSuffix(one);
        two = removeSuffix(two);
        for(int k=0;k<replaceable.length;k++){
            one = one.replaceAll(replaceable[k][0], replaceable[k][1]);
            two = two.replaceAll(replaceable[k][0], replaceable[k][1]);
        }
        return Levenshtein(one, two);
    }
    public static float relative(String one,String two){
        if(one.equals(two)) return 1;
        one = removeSuffix(one);
        two = removeSuffix(two);
        for(int k=0;k<replaceable.length;k++){
            one = one.replaceAll(replaceable[k][0], replaceable[k][1]);
            two = two.replaceAll(replaceable[k][0], replaceable[k][1]);
        }
        return 1 - (float)Levenshtein(one,two)*2/(one.length()+two.length());
    }
    private static int Levenshtein(String one, String two){
        int L1=one.length()+1,L2=two.length()+1;
        int[][] mat=new int[L1][L2];
        StringBuilder sb1=new StringBuilder(one),sb2=new StringBuilder(two);
        for(int i=0;i<L1;i++) mat[i][0]=i;
        for(int j=0;j<L2;j++){
            mat[0][j]=j;
            for(int i=1;i<L1 && j>0;i++){
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
    private static Pattern pattern = Pattern.compile("[А-яA-z0-9\\-']+|(\\{\\s*[A-z0-9]+\\s*:\\s*[A-z]+\\s*\\})");
    public static ArrayList<String> toRuleWords(String text){
        ArrayList<String> str_list=new ArrayList<String>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            str_list.add(matcher.group().replace(" ",""));
        }
        return str_list;
    }
}