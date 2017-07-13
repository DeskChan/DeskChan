package info.deskchan.speech_command_system;

import java.util.ArrayList;

public class TextOperations {
    private static char[][] replaceable=new char[][]{
        {'о','а'} , {'е','и'} , {'д','т'} , {'г','к'} , {'ж','ш'} , {'ы','и'} , {'з','с'} , {'б','п'} , {'в','ф'}
    };
    public static int BorderedLevenshtein(String one,String two){
        try{
            int L=Math.min(one.length(),two.length());
            if(one.substring(0,L)==two.substring(0,L)) return 0;
            L++;
			int[][] mat=new int[L][L];
            StringBuilder sb1=new StringBuilder(one),sb2=new StringBuilder(two);
            for(int i=0;i<L;i++){
                for(int k=0;k<replaceable.length && i<L-1;k++)
                    if(sb1.charAt(i)==replaceable[k][0]) sb1.setCharAt(i,replaceable[k][1]);
                mat[i][0]=i;
            }
            for(int j=1;j<L;j++){
                for(int k=0;k<replaceable.length && j<L-1;k++)
                    if(sb2.charAt(j)==replaceable[k][0]) sb2.setCharAt(j,replaceable[k][1]);
                mat[0][j]=j;
                for(int i=1;i<L && j>0;i++){
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
            return mat[L-1][L-1];
        } catch(Exception e){
            return 0;
        }
    }
    public static int Levenshtein(String one,String two){
        if(one==two) return 0;
        int L1=one.length()+1,L2=two.length()+1;
        int[][] mat=new int[L1][L2];
        StringBuilder sb1=new StringBuilder(one),sb2=new StringBuilder(two);
        for(int i=0;i<L1;i++){
            for(int k=0;k<replaceable.length && i<L1-1;k++)
                if(sb1.charAt(i)==replaceable[k][0]) sb1.setCharAt(i,replaceable[k][1]);
            mat[i][0]=i;
        }
        for(int j=0;j<L2;j++){
            for(int k=0;k<replaceable.length && j<L2-1;k++)
                if(sb2.charAt(j)==replaceable[k][0]) sb2.setCharAt(j,replaceable[k][1]);
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
    public static float Similar(String one,String two){
        return 1-(float)Levenshtein(one,two)/(Math.max(one.length(),two.length()));
    }
    public static ArrayList<String> toClearWords(String text){
        ArrayList<String> str_list=new ArrayList<String>();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<text.length();i++){
            if(Character.isLetterOrDigit(text.charAt(i))){
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
}
