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

    /** Converts text to map. <br> Example: 'key1: value1, key2: "value 22" value21' **/
    public static Map<String,Object> toMap(String text){
        TagsMap tags = new TagsMap(text);
        Map<String,Object> map = new HashMap<>();

        for(HashMap.Entry<String, List<String>> entry : tags.entrySet()){
            if(entry.getValue().size() == 0) continue;
            if(entry.getValue().size() == 1)
                map.put(entry.getKey(), entry.getValue().get(0));
            else
                map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public static class TagsMap{

        private Map<String, List<String>> tags;

        public TagsMap() {
            tags = new HashMap<>();
        }

        public TagsMap(String text) {
            this();
            put(text);
        }

        public void put(String tag, List<String> args) {
            if (args.size() > 0) tags.put(tag, args);
        }

        /** Split input values string to list. **/
        private static ArrayList<String> split(String text) {
            ArrayList<String> args = new ArrayList<String>();
            if (text == null || text.length() < 3) return args;

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

        public void put(String tag, String args) {
            try {
                List<String> list = split(args);
                if (list.size() > 0) tags.put(tag, list);
            } catch (Exception e) {
                throw new IllegalArgumentException(args);
            }
        }

        public boolean put(String text) {
            if (text == null || text.length() == 0) return true;
            boolean inQuoteMarks = false;
            boolean before = true;
            text = text.replace("\n", "") + ",";
            String tagname = "";
            int st = 0;
            for (int c = 0; c < text.length(); c++) {
                if (!before) {
                    if (text.charAt(c) == ',') {
                        ArrayList<String> list = new ArrayList<>();
                        tags.put(tagname, split(text.substring(st, c)));
                        st = c + 1;
                        tagname = "";
                        before = true;
                    }
                } else if (text.charAt(c) == '"') inQuoteMarks = !inQuoteMarks;
                else if (text.charAt(c) == ':' && !inQuoteMarks) {
                    if (st == c) {
                        st = c + 1;
                        continue;
                    }
                    if (text.charAt(st) == '"' && text.charAt(c - 1) == '"')
                        tagname = text.substring(st + 1, c - 1);
                    else tagname = text.substring(st, c);
                    tagname = tagname.trim();
                    st = c + 1;
                    before = false;
                }
            }
            return true;
        }

        public String getAsString(String tag) {
            if (!tags.containsKey(tag)) return "";

            StringBuilder sb = new StringBuilder();
            for (String arg : tags.get(tag)) {
                sb.append("\"");
                sb.append(arg);
                sb.append("\" ");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        public List<String> get(String tag) { return tags.get(tag); }

        public void remove(String tag) {
            tags.remove(tag);
        }

        public Set<String> keySet() {
            return tags.keySet();
        }

        public Set<HashMap.Entry<String, List<String>>> entrySet() {
            return tags.entrySet();
        }

        public boolean match(String tag, List<String> args) {
            List<String> targs = get(tag);
            if (targs == null)
                return (args.size() == 0);

            for (String arg : args)
                if (!targs.contains(arg)) return false;

            return true;
        }

        public boolean match(String tag, String argstext) {
            return match(tag, split(argstext));
        }

        public Map<String, List<String>> toMap() {
            return new HashMap<>(tags);
        }

        @Override
        public String toString() {
            if (tags.size() == 0) return "";

            StringBuilder sb = new StringBuilder();
            for (String key : tags.keySet()) {
                sb.append(key);
                sb.append(":");
                sb.append(getAsString(key));
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            return sb.toString();
        }

        public void clear(){
            tags.clear();
        }
    }
}