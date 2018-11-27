package info.deskchan.talking_system;

import java.util.*;

/** Class representing tags map. Each value of map can be only Set&lt;String&gt;, including empty list. <br>
 * You can parse map from string like <br> key1: value1, key2, key3: "value 21" value22 !value23. <br><br>
 *
 * '!' means boolean negation, but '!value' will override 'value'.
 * Keys also can be negated, but negated keys cannot contain lists.**/
public class TagsMap implements Map<String, Set<String>> {

    // null as value means keys without value like "key2"
    private Map<String, Set<String>> tags;

    public TagsMap() { tags = new HashMap<>();  }

    public TagsMap(String text) {
        this();
        if (text == null) return;
        putFromText(text);
    }

    public TagsMap(TagsMap map) {
        this();
        if (map == null) return;
        for (Map.Entry<String, Set<String>> entry : map.entrySet()){
            put(entry.getKey(), new ImprovedSet<>(entry.getValue()));
        }
    }

    public TagsMap(Map<String, Object> map) {
        this();
        if (map == null) return;
        for (Map.Entry<String, Object> entry : map.entrySet()){
            if (entry.getValue() instanceof Collection){
                Set<String> set = new ImprovedSet<>();
                for (Object item : (Collection) entry.getValue()){
                    set.add(item.toString());
                }
                put(entry.getKey(), set);
            } else {
                put(entry.getKey(), entry.getValue().toString());
            }
        }
    }

    public Set<String> put(String tag) {
        return put(tag, (Collection) null);
    }

    public Set<String> put(String tag, Collection args) {
        if (tag.startsWith("!")){
           if (args != null)
               throw new IllegalArgumentException("Negated tag with not null list specified");
           else
               remove(tag.substring(1));
        } else remove("!" + tag);

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
                return put(tag, list);
            else
                return put(tag, (Collection) null);
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
                    put(text.substring(st, c).trim(), (Collection) null);
                else
                    put(tagName, split(text.substring(st, c).trim()));

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

        return tags.get(tag).toString();
    }

    public String getRandom(String tag){
        Set<String> set = get(tag);
        if (set == null || set.size() == 0)
            return null;
        Iterator<String> it = set.iterator();
        for (int i = 0, l = new Random().nextInt(set.size()); i < l; i++)
            it.next();
        return it.next();
    }

    private static boolean containsPositive(Collection<String> items){
        if (items == null) return false;
        for (String item : items)
            if (item.charAt(0) != '!') return true;
        return false;
    }

    public boolean match(String tag, Collection<String> args) {
        // Check if negations is present
        if (tag.startsWith("!")){
            if (containsKey(tag.substring(1))) return false;
        } else {
            if (containsKey("!" + tag)) return false;
            if (args == null || args.size() == 0) {
                return tags.containsKey(tag);
            }
        }

        Set<String> dstTags = tags.get(tag);
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
        return match(new TagsMap(argstext));
    }

    public boolean match(Map other) {
        for (Object tag : other.keySet()) {
            Object val = other.get(tag);
            if (val instanceof Collection || val == null) {
                if (!match(tag.toString(), (Collection) other.get(tag))) return false;
            } else {
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
    public void putAll(Map<? extends String, ? extends Set<String>> map){
        for (Map.Entry<? extends String, ? extends Set<String>> entry : map.entrySet()){
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Set<String> put(String tag, Set<String> args) {
        return put(tag, (Collection) args);
    }

    @Override public Set<String> keySet() {  return tags.keySet();  }

    @Override public Set<Map.Entry<String, Set<String>>> entrySet() {  return tags.entrySet();  }

    @Override public int size(){  return tags.size();  }

    @Override public int hashCode(){  return tags.hashCode();  }

    @Override public boolean containsValue(Object value){  return tags.containsValue(value);  }

    @Override public boolean isEmpty(){  return tags.isEmpty();  }

    @Override public boolean containsKey(Object key){ return tags.containsKey(key); }

    @Override public boolean equals(Object o){
        if (o == null) return false;

        TagsMap compare;
        if (o instanceof String)
            compare = new TagsMap((String) o);
        else if (o instanceof TagsMap)
            compare = (TagsMap) o;
        else return false;

        return this.size() == compare.size() && this.match(compare) && compare.match(this);
    }

    @Override public Collection<Set<String>> values() {  return tags.values();  }

    @Override public Set<String> remove(Object item){  return tags.remove(item);  }

    @Override public Set<String> get(Object key){  return tags.get(key);  }

    @Override public void clear(){  tags.clear();  }

    private static class ImprovedSet<E> extends HashSet<E> {
        public ImprovedSet(){
            super();
        }
        public ImprovedSet(Collection copy){
            if (copy == null) return;
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

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            for (E arg : this) {
                sb.append("\"");
                sb.append(arg.toString());
                sb.append("\" ");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

    public int dataHashCode(){
        int hash = 0;
        for (String key : tags.keySet()){
            hash += key.hashCode() + tags.get(key).hashCode();
        }
        return hash;
    }
}
