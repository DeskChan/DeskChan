package info.deskchan.core_utils;

import java.util.*;

public class TextOperations {
    private final static char[][] simplify = {{'ъ', 'й'}, {'ь', 'й'}, {'ы', 'и'}, {'ё', 'е'}};

    public static ArrayList<String> simplifyWords(String[] words) {
        ArrayList<String> w = new ArrayList<>();
        String cw;
        for (int i = 0; i < words.length; i++) {
            cw = simplifyWord(words[i]);
            if (cw.equals("не") && i < words.length - 1) {
                String cw2 = simplifyWord(words[i + 1]);
                if (!cw2.equals("не")) {
                    w.add(cw + cw2);
                    i++;
                    continue;
                }
            }
            w.add(cw);
        }
        return w;
    }

    public static ArrayList<String> simplifyWords(ArrayList<String> words) {
        ArrayList<String> w = new ArrayList<>();
        String cw;
        for (int i = 0; i < words.size(); i++) {
            cw = simplifyWord(words.get(i));
            if (cw.equals("не") && i < words.size() - 1) {
                String cw2 = simplifyWord(words.get(i + 1));
                if (!cw2.equals("не")) {
                    w.add(cw + cw2);
                    i++;
                    continue;
                }
            }
            w.add(cw);
        }
        return w;
    }

    public static String simplifyWord(String word) {
        StringBuilder sb = new StringBuilder(word);
        for (int i = 0; i < sb.length(); i++) {
            if (i > 0 && sb.charAt(i) == sb.charAt(i - 1)) {
                sb.deleteCharAt(i);
                i--;
                continue;
            }
            for (int k = 0; k < simplify.length; k++)
                if (sb.charAt(i) == simplify[k][0]) {
                    sb.setCharAt(i, simplify[k][1]);
                    break;
                }
        }
        return sb.toString();
    }

    public static ArrayList<String> extractWords(String phrase) {
        return extractWords(phrase, 0);
    }

    public static ArrayList<String> extractWordsLower(String phrase) {
        return extractWords(phrase, 1);
    }

    public static ArrayList<String> extractWordsUpper(String phrase) {
        return extractWords(phrase, 2);
    }

    private static ArrayList<String> extractWords(String phrase, int type) {
        ArrayList<String> words = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= phrase.length(); i++) {
            if (i==phrase.length() || phrase.charAt(i) == ' ' || phrase.charAt(i) == '\n') {
                if (sb.length() == 0) continue;
                words.add(sb.toString());
                sb = new StringBuilder();
            } else if (Character.isLetter(phrase.charAt(i)) || Character.UnicodeBlock.of(phrase.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
                switch (type) {
                    case 0:
                        sb.append(phrase.charAt(i));
                        break;
                    case 1:
                        sb.append(Character.toLowerCase(phrase.charAt(i)));
                        break;
                    case 2:
                        sb.append(Character.toUpperCase(phrase.charAt(i)));
                        break;
                }
            }
        }
        return words;
    }

    /** Class representing tags map. Each value of map can be only Set&lt;String&gt;, including empty list. <br>
     * You can parse map from string like <br> key1: value1, key2, key3: "value 21" value22 !value23. <br><br>
     * It will definitely throw an error if you create an instance with type definition other than String, Set. <br>
     * '!' means boolean negation, but '!value' will override 'value'. **/
    public static class TagsMap<K, V> implements Map<K, V>{

        // null as value means keys without value like "key2"
        private Map<String, Set<String>> tags;

        public TagsMap() { tags = new HashMap<>();  }

        public TagsMap(String text) {
            this();
            putFromText(text);
        }

        public TagsMap(Map<String, Object> map) {
            this();
            for (Map.Entry<String, Object> entry : map.entrySet()){
                if (entry.getValue() instanceof Collection){
                    Set<String> set = new ImprovedSet<>();
                    for (Object item : (Collection) entry.getValue()){
                        set.add(item.toString());
                    }
                } else {
                    put(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        public Set<String> put(String tag) {
            return tags.put(tag, null);
        }

        public Set<String> put(String tag, Collection args) {
            if (args != null && args.size() > 0)
                return tags.put(tag, new ImprovedSet<>(args));
            else
                return tags.put(tag, null);
        }

        /** Split input values string to list. <br>
         * Example: 'value1 "value2 with spaces" value3'.  **/
        private static Set<String> split(String text) {
            Set<String> args = new ImprovedSet<>();
            if (text == null || text.length() == 0) return args;

            boolean inQuoteMarks = false;
            int startPos = 0;
            for (int c = 0; c < text.length(); c++) {
                if (text.charAt(c) == '"') inQuoteMarks = !inQuoteMarks;
                else if (text.charAt(c) == ' ' && !inQuoteMarks) {
                    if (startPos == c) {
                        startPos = c + 1;
                        continue;
                    }
                    if (text.charAt(startPos) == '"' && text.charAt(c - 1) == '"')
                        args.add(text.substring(startPos + 1, c - 1));
                    else
                        args.add(text.substring(startPos, c));
                    startPos = c + 1;
                }
            }
            if (startPos < text.length()) {
                int a = startPos + (text.charAt(startPos) == '"' ? 1 : 0);
                int b = text.length() - (text.charAt(text.length() - 1) == '"' ? 1 : 0);
                args.add(text.substring(a, b));
            }
            return args;
        }

        public Set<String> put(String tag, String args) {
            try {
                Set<String> list = split(args);
                if (list.size() > 0)
                    return tags.put(tag, list);
                else
                    return tags.put(tag, null);
            } catch (Exception e) {
                throw new IllegalArgumentException(args);
            }
        }

        public void putFromText(String text) {
            if (text == null || text.length() == 0) return;

            boolean inQuoteMarks = false, beforeColon = true;
            text = text.replace("\n", "") + ",";
            String tagName = "";
            int st = 0;

            for (int c = 0; c < text.length(); c++) {
                 if (text.charAt(c) == ',') {
                     if (beforeColon)
                         tags.put(text.substring(st, c).trim(), null);
                     else
                         tags.put(tagName, split(text.substring(st, c).trim()));

                     st = c + 1;
                     tagName = "";
                     beforeColon = true;
                } else if (text.charAt(c) == '"') inQuoteMarks = !inQuoteMarks;
                else if (text.charAt(c) == ':' && !inQuoteMarks) {
                    if (st == c) {
                        st = c + 1;
                        continue;
                    }
                    if (text.charAt(st) == '"' && text.charAt(c - 1) == '"')
                        tagName = text.substring(st + 1, c - 1);
                    else tagName = text.substring(st, c);
                    tagName = tagName.trim();
                    st = c + 1;
                    beforeColon = false;
                }
            }
        }

        public String getAsString(String tag) {
            if (!tags.containsKey(tag)) return null;
            if (tags.get(tag) == null)  return "";

            StringBuilder sb = new StringBuilder();
            for (String arg : tags.get(tag)) {
                sb.append("\"");
                sb.append(arg);
                sb.append("\" ");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        public String getFirst(Object key){
            return tags.get(key).iterator().next();
        }

        private static boolean containsPositive(Collection<String> items){
            if (items == null) return false;
            for (String item : items)
                if (item.charAt(0) != '!') return true;
            return false;
        }

        public boolean match(String tag, Collection<String> args) {
            Set<String> dstTags = tags.get(tag);
            if (args == null || args.size() == 0) {
                if (dstTags == null) return true;
                for (String arg : dstTags)
                    if (arg.charAt(0) != '!') return false;

                return true;
            }

            boolean containsAnyPositive = containsPositive(dstTags),
                    containsAnyNegative = dstTags != null && !containsAnyPositive;
            if (!containsAnyNegative && !containsAnyPositive) return !containsPositive(args);

            for (String arg : args){
                boolean containsPositive = false, containsNegative = false;
                boolean negation = arg.charAt(0) == '!';
                if (negation) arg = arg.substring(1);
                String narg = "!" + arg;
                for (String arg2 : dstTags){
                    if (arg2.equals(arg)) containsPositive = true;
                    else if (arg2.equals(narg)) containsNegative = true;
                }
                if (!negation){
                    if (!containsPositive || containsNegative) return false;
                } else {
                    if (containsPositive) return false;
                }
            }

            return true;
        }

        public boolean match(String tag, String argstext) {
            return match(tag, split(argstext));
        }

        public boolean match(String argstext) {
            return match(new TagsMap<>(argstext));
        }

        public boolean match(Map other) {
            for (Object tag : other.keySet()) {
                if (other.get(tag) instanceof Collection) {
                    if (!match(tag.toString(), (Collection) other.get(tag))) return false;
                } else {
                    if (other.get(tag) == null) {
                        if (containsKey(tag)) continue;
                        else return false;
                    }
                    if (!match(tag.toString(), other.get(tag).toString())) return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            if (tags.size() == 0) return "";

            StringBuilder sb = new StringBuilder();
            for (String key : tags.keySet()) {
                sb.append(key);
                String args = getAsString(key);
                if (args.length() > 0) {
                    sb.append(":");
                    sb.append(args);
                }
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            return sb.toString();
        }

        /* -- Fully implementing interface, don't mind -- */

        @Override
        public void putAll(Map<? extends K, ? extends V> map){
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()){
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public V put(K tag, V args) {
            if (args instanceof Collection)
                return (V) put(tag.toString(), (Collection) args);
            else
                return (V) put(tag.toString(), args.toString());
        }

        @Override public Set<K> keySet() {  return (Set<K>) tags.keySet();  }

        @Override public Set<HashMap.Entry<K, V>> entrySet() {  return (Set) tags.entrySet();  }

        @Override public int size(){  return tags.size();  }

        @Override public int hashCode(){  return tags.hashCode();  }

        @Override public boolean containsKey(Object key){  return tags.keySet().contains(key);  }

        @Override public boolean containsValue(Object value){  return tags.containsValue(value);  }

        @Override public boolean isEmpty(){  return tags.isEmpty();  }

        @Override public boolean equals(Object other){  return tags.equals(other);  }

        @Override public Collection<V> values() {  return (Collection<V>) tags.values();  }

        @Override public V remove(Object item){  return (V) tags.remove(item);  }

        @Override public V get(Object key){  return (V) tags.get(key);  }

        @Override public void clear(){  tags.clear();  }

        private static class ImprovedSet<E> extends HashSet<E>{
            public ImprovedSet(){
                super();
            }
            public ImprovedSet(Collection copy){
                for (Object item : copy) add(item.toString());
            }

            public boolean add(String item){
                String repr = item;
                if (repr.charAt(0) == '!')
                    repr = repr.substring(1);
                if (contains(repr))
                    remove(repr);
                return super.add((E) item);
            }

            @Override
            public boolean add(E item){
                return add(item.toString());
            }
        }

        private static void Testing(){
            TagsMap map = new TagsMap("part: head legs");
            System.out.println(map);
            System.out.println(map.match("part: legs"));
            System.out.println(map.match("part", "head"));
            System.out.println(map.match("part", "spine"));
            System.out.println(map.match("part", new ArrayList<String>(){{
                add("!spine");
            }}));
            System.out.println(map.match("part: legs head"));
            System.out.println(map.match("part"));
            map = new TagsMap("species: ai, sleepTime");
            System.out.println(map);
            System.out.println(map.match("sleepTime"));
            System.out.println(map.match("dayTime"));
            System.out.println(map.match("sleepTime, species: !android"));
            System.out.println(map.match("sleepTime, species: android"));
        }
    }
}