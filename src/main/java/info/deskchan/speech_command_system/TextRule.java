package info.deskchan.speech_command_system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TextRule {
    static class Element{
        String word = null;
        Argument argument = null;
        Element(String text){
            argument = Argument.create(text);
            if(argument == null) word = text;
        }
        boolean isWord(){
            return word != null;
        }
    }
    public static class MatchResult{
        float matchPercentage;
        int wordsUsed;
        int firstWordUsed;
        boolean[] used;
        MatchResult(int len){
            used = new boolean[len];
            for(int i=0;i<len;i++) used[i]=false;
            firstWordUsed = len;
            wordsUsed = 0;
            matchPercentage = 0;
        }
        public boolean better(MatchResult other){
            return wordsUsed>=other.wordsUsed && matchPercentage>=other.matchPercentage && firstWordUsed<=other.firstWordUsed;
        }
        @Override
        public String toString(){
            return "words used: "+wordsUsed+", first word used at: "+firstWordUsed+", match: "+matchPercentage+"%";
        }
    }
    MatchResult result;
    ArrayList<Element> elements = new ArrayList<>();
    public TextRule(String text){
        ArrayList<String> words = PhraseComparison.toRuleWords(text);
        for(String word : words)
            elements.add(new Element(word));
    }
    public void match(ArrayList<String> words){
        result = new MatchResult(words.size());
        int words_count = 0;
        for(Element element : elements){
            if(!element.isWord()) continue;
            words_count++;
            float cur_res=0;
            int cur_pos=-1;
            for(int i=0;i<words.size();i++){
                if(result.used[i]) continue;
                float relative = PhraseComparison.relative(words.get(i), element.word);
                if(relative>0.7 && relative>cur_res){
                    cur_res=relative;
                    cur_pos=i;
                }
            }
            if(cur_pos<0) continue;
            result.wordsUsed++;
            result.matchPercentage+=cur_res;
            result.used[cur_pos]=true;
            if(result.firstWordUsed>cur_pos) result.firstWordUsed=cur_pos;
        }
        result.matchPercentage/=words_count;
    }
    public HashMap<String,Object> getArguments(String text, ArrayList<String> words){
        HashMap<String,Object> arguments=new HashMap<>();
        for(Element element : elements){
            if(element.isWord()) continue;
            element.argument.localize(text, words, result.used);
        }
        for(Element element : elements){
            if(element.isWord()) continue;
            arguments.put(element.argument.name, element.argument.value);
        }
        return arguments;
    }
}

class Argument {
    private enum ArgumentType { Text , Word , List , Integer, Number , Date, Time, DateTime, RelativeDateTime}

    public String name;
    public ArgumentType type;
    public Object lastWord;
    Object value;
    private Argument(String name, ArgumentType type){
        this.name=name;
        this.type=type;
        lastWord=null;
        value=null;
    }
    public static ArgumentType searchArgument(String search) {
        for (ArgumentType each : ArgumentType.class.getEnumConstants())
            if (each.name().compareToIgnoreCase(search) == 0)
                return each;

        return null;
    }
    public static Argument create(String text){
        if(text.charAt(0)!='{') return null;
        String[] parts=text.substring(1,text.length()-1).split(":");
        try {
            parts[1] = parts[1].toLowerCase();
            return new Argument(parts[0], searchArgument(parts[1]));
        } catch (Exception e){
            return null;
        }
    }
    private String getLastWord() {
        if(lastWord==null) return null;
        if(lastWord instanceof String) return (String)lastWord;
        return ((Argument)lastWord).getLastWord();
    }
    public void localize(String text, ArrayList<String> words, boolean[] used){
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
            case Text: {
                String textCopy=text;
                for (int k=0; k < words.size(); k++) {
                    if (k < i || used[k]) {
                        textCopy=textCopy.replace(words.get(k),"");
                    }
                    used[k] = true;
                }
                value=textCopy.trim();
            } break;
            case Word:{
                value=lastWord=words.get(i);
                used[i]=true;
            } break;
            case List: {
                List<String> list=new ArrayList<>();
                for (; i < words.size(); i++)
                    if (!used[i]){
                        list.add(words.get(i));
                        used[i]=true;
                        lastWord=words.get(i);
                    }
                value=list;
            } break;
            case Integer:{
                value = Parsers.parseInteger(words, used);
            } break;
            case Number:{
                value = Parsers.parseNumber(words, used);
            } break;
            case DateTime:{
                value = Parsers.parseDateTime(words, used);
            } break;
            case Time:{
                value = Parsers.parseTime(words, used);
            } break;
            case Date:{
                value = Parsers.parseDate(words, used);
            } break;
            case RelativeDateTime:{
                value = Parsers.parseRelativeDateTime(words, used);
            } break;
        }
    }
}
