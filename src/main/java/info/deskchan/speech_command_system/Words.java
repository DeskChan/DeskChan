package info.deskchan.speech_command_system;

import java.util.ArrayList;

class Pair<K, V>{
    K one; V two;
    Pair(K key, V value){
        one = key; two = value;
    }
}

/** Class representing words array with info about their usage.
 * 'Usage' means that word was already parsed and interpreted as it should be so we do not parse it again. **/
public class Words {
    ArrayList<String> words;
    boolean[] used;
    protected static boolean[] arrayCopy(boolean[] from, int start, int len){
        boolean[] array = new boolean[len];
        System.arraycopy(from, start, array, 0, len);
        return array;
    }
    public Words(ArrayList<String> words, boolean[] used){
        this.words = (ArrayList<String>) words.clone();
        this.used = arrayCopy(used, 0, words.size());
    }
    public Words(ArrayList<String> words){
        this.words = (ArrayList<String>) words.clone();
        this.used = new boolean[words.size()];
        for(int i=0;i<this.used.length;i++) this.used[i] = false;
    }
    public Words(ArrayList<String> words, int start, int end){
        this.words = new ArrayList<>(words.subList(start, end));
        this.used = new boolean[end - start];
        for(int i=0;i<this.used.length;i++) this.used[i] = false;
    }
    public Words(ArrayList<String> words, boolean[] used, int start, int end){
        this.words = new ArrayList<>(words.subList(start, end));
        this.used = arrayCopy(used, start, end - start);
    }
    public Words(Words clone){
        this.words = (ArrayList<String>) clone.words.clone();
        this.used = arrayCopy(clone.used, 0, words.size());
    }
    public Words(Words clone, int start, int end){
        this.words = new ArrayList<>(clone.words.subList(start, end));
        this.used = arrayCopy(clone.used, start, end - start);
    }
    public int join(boolean[] other){
        return join(other, 0);
    }
    public int join(boolean[] other, int start){
        int last = 0;
        for(int i = start, k=0; k<other.length; i++, k++) {
            if(!used[i] && other[k])
                last = i;
            used[i] = used[i] | other[k];
        }
        return last;
    }
    public int size(){ return words.size(); }
    public String get(int i){ return words.get(i); }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<words.size(); i++)
            sb.append(words.get(i)+" "+used[i]+" / ");
        return sb.toString();
    }
}
