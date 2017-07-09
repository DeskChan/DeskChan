package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MeaningExtractor {
    private ArrayList<String> meaningNames;
    private HashMap<String,HashMap<Integer,Integer>> meanings;
    private HashMap<Integer,Float> normalizeMeaningTable(HashMap<Integer,Integer> table){
        float sum=0;
        for(HashMap.Entry<Integer,Integer> entry : table.entrySet())
            sum+=entry.getValue();
        HashMap<Integer,Float> normalized=new HashMap<>();
        for(HashMap.Entry<Integer,Integer> entry : table.entrySet())
            normalized.put(entry.getKey(),entry.getValue()/sum);
        return normalized;
    }
    public MeaningExtractor(){
        meaningNames=new ArrayList<>();
        meanings=new HashMap<>();
        load();
    }
    public String analyze(String phrase){
        ArrayList<String> words= TextOperations.simplifyWords(TextOperations.extractWordsLower(phrase));
        float max=0;
        int index=-1;
        int count=0;
        HashMap<Integer,Float> phrase_meanings=new HashMap<>();
        HashMap<Integer,Integer> mp;
        for(String word : words){
            mp=meanings.getOrDefault(word,null);
            if(mp==null || mp.size()==0) continue;
            count++;
            for(HashMap.Entry<Integer,Float> entry : normalizeMeaningTable(mp).entrySet()){
                float f=phrase_meanings.getOrDefault(entry.getKey(),0f)+entry.getValue();
                phrase_meanings.put(entry.getKey(),f);
                if(f>max) {
                    max = f;
                    index = entry.getKey();
                }
            }
        }
        if(index<0 || max/count<0.5) return null;
        return meaningNames.get(index);
    }
    public void teach(String phrase,String meaning){
        ArrayList<String> words=TextOperations.simplifyWords(TextOperations.extractWordsLower(phrase));
        meaning=meaning.toUpperCase();
        int meanIndex=meaningNames.indexOf(meaning);
        if(meanIndex<0){
            meanIndex=meaningNames.size();
            meaningNames.add(meaning);
        }
        HashMap<Integer,Integer> mp;
        for(String word : words){
            mp=meanings.getOrDefault(word,null);
            if(mp==null){
                mp=new HashMap<Integer,Integer>();
                meanings.put(word,mp);
            }
            int a=mp.getOrDefault(meanIndex,0)+1;
            mp.put(meanIndex,a);
        }
    }
    public void print(){
        for(Map.Entry<String,HashMap<Integer,Integer>> word : meanings.entrySet()){
            System.out.println(word.getKey());
            for(Map.Entry<Integer,Integer> meaning : word.getValue().entrySet()){
                System.out.println("  "+meaningNames.get(meaning.getKey())+" "+meaning.getValue());
            }
        }
    }
    public void save(){
        FileWriter writer=null;
        try {
            writer=new FileWriter(Main.getDataDirPath().resolve("meanings").toFile());
        } catch(Exception e){
            Main.log("Error while locate space for meanings file");
            return;
        }
        BufferedWriter out=new BufferedWriter(writer);
        try {
            for(String name : meaningNames)
                out.write(name+"\n");
            out.write("\n");
            for(HashMap.Entry<String,HashMap<Integer,Integer>> entry : meanings.entrySet()) {
                if(entry.getValue()==null || entry.getValue().entrySet().size()==0) continue;
                out.write(entry.getKey());
                for(HashMap.Entry<Integer,Integer> mean : entry.getValue().entrySet()) {
                    out.write(" "+entry.getKey()+" "+entry.getValue());
                }
                out.write("\n");
            }
            out.flush();
            out.close();
        } catch(Exception e){
            Main.log("Error while writing meanings file");
            return;
        }
    }
    public void load(){
        FileReader reader=null;
        try {
            reader=new FileReader(Main.getDataDirPath().resolve("meanings").toFile());
        } catch(Exception e){
            Main.log("Error while locating meanings file");
            return;
        }
        BufferedReader out=new BufferedReader(reader);
        try {
            String line;
            while((line=out.readLine())!=null){
                if(line.length()<1) break;
                meaningNames.add(line);
            }
            String[] parts;
            HashMap<Integer,Integer> map;
            while((line=out.readLine())!=null){
                if(line.length()<1) break;
                parts=line.split(" ");
                map=new HashMap<>();
                for(int i=0;i<parts.length-2;i+=2){
                    int i1=-1,i2=-1;
                    do{
                        i++;
                        try{
                            i1=Integer.parseInt(parts[i]);
                        } catch(Exception e){ continue; }
                        break;
                    } while(i<parts.length-1);
                    if(i1<0) break;
                    do{
                        i++;
                        try{
                            i2=Integer.parseInt(parts[i]);
                        } catch(Exception e){ continue; }
                        break;
                    } while(i<parts.length);
                    if(i2<0) break;
                    map.put(i1,i2);
                }
                if(map.size()>0){
                    meanings.put(parts[0],map);
                }
            }
            out.close();
        } catch(Exception e){
            Main.log("Error while writing meanings file");
            return;
        }
    }

}