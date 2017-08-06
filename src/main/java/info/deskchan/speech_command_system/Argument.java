package info.deskchan.speech_command_system;

import java.util.ArrayList;
import java.util.List;

public class Argument {
    private static String[] arguments = new String[]{ "text" , "word" , "list" };

    public String name;
    public int type;
    public Object lastWord;
    Object value;
    private Argument(String name,int type){
        this.name=name;
        this.type=type;
        lastWord=null;
        value=null;
    }
    public static Argument create(String text){
        if(text.charAt(0)!='{') return null;
        String[] parts=text.substring(1,text.length()-1).split(":");
        for(int i=0;i<arguments.length;i++){
            if(arguments[i].equals(parts[1])) return new Argument(parts[0],i);
        }
        return null;
    }
    private String getLastWord() {
        if(lastWord==null) return null;
        if(lastWord instanceof String) return (String)lastWord;
        return ((Argument)lastWord).getLastWord();
    }
    public void localize(List<String> words, boolean[] used){
        String startword=getLastWord();
        int i = 0;
        if(startword!=null) {
            for (i = 0; i < words.size(); i++)
                if (words.get(i).equals(startword)){
                    do{
                        i++;
                    } while(i<words.size() && used[i]);
                    break;
                }
            if (i == words.size()) return;
        }
        switch(type){
            case 0: {
                StringBuilder text=new StringBuilder();
                for (; i < words.size(); i++)
                    if (!used[i]){
                        text.append(words.get(i));
                        text.append(" ");
                        used[i]=true;
                        lastWord=words.get(i);
                    }
                if(text.length()>1){
                    text.deleteCharAt(text.length()-1);
                    value=text.toString();
                }
            } break;
            case 1:{
                value=lastWord=words.get(i);
                used[i]=true;
            } break;
            case 2: {
                List<String> text=new ArrayList<>();
                for (; i < words.size(); i++)
                    if (!used[i]){
                        text.add(words.get(i));
                        used[i]=true;
                        lastWord=words.get(i);
                    }
                value=text;
            } break;
        }
    }
}
