package info.deskchan.core;

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
        for (int i = 0; i < phrase.length(); i++) {
            if (phrase.charAt(i) == ' ' || phrase.charAt(i) == '\n') {
                if (sb.length() == 0) continue;
                words.add(sb.toString());
                sb = new StringBuilder();
            }
            if (Character.UnicodeBlock.of(phrase.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
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

    public static Map<String,Object> toMap(String text){
        TagsContainer tags=new TagsContainer(text);
        Map<String,Object> map=new HashMap<>();
        for(HashMap.Entry<String, List<String>> entry : tags.entrySet()){
            if(entry.getValue().size()==0) continue;
            if(entry.getValue().size()==1)
                map.put(entry.getKey(),entry.getValue().get(0));
            else
                map.put(entry.getKey(),entry.getValue());
        }
        return map;
    }
    public static class TagsContainer {
        private HashMap<String, List<String>> tags;

        public TagsContainer() {
            tags = new HashMap<>();
        }

        public TagsContainer(String text) {
            tags = new HashMap<>();
            put(text);
        }

        public boolean put(String tag, List<String> args) {
            tags.put(tag, args);
            return true;
        }

        private static ArrayList<String> split(String text) {
            ArrayList<String> args = new ArrayList<String>();
            boolean inQuoteMarks = false;
            int st = 0;
            for (int c = 0; c < text.length(); c++) {
                if (text.charAt(c) == '"') inQuoteMarks = !inQuoteMarks;
                else if (text.charAt(c) == ' ' && !inQuoteMarks) {
                    if (st == c) {
                        st = c + 1;
                        continue;
                    }
                    if (text.charAt(st) == '"' && text.charAt(c - 1) == '"')
                        args.add(text.substring(st + 1, c - 1));
                    else
                        args.add(text.substring(st, c));
                    st = c + 1;
                }
            }
            if (st < text.length()) {
                int a = st + (text.charAt(st) == '"' ? 1 : 0);
                int b = text.length() - (text.charAt(text.length() - 1) == '"' ? 1 : 0);
                args.add(text.substring(a, b));
            }
            return args;
        }

        public boolean put(String tag, String args) {
            try {
                tags.put(tag, split(args));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public boolean put(String text) {
            if (text == null || text.length() == 0) return true;
            boolean inQuoteMarks = false;
            boolean before = true;
            text = text.replace("\n", "") + ",";
            String tagname = new String();
            int st = 0;
            for (int c = 0; c < text.length(); c++) {
                if (!before) {
                    if (text.charAt(c) == ',') {
                        ArrayList<String> list = new ArrayList<>();
                        tags.put(tagname, split(text.substring(st, c)));
                        st = c + 1;
                        tagname = new String();
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
            if (tags.containsKey(tag)) {
                StringBuilder sb = new StringBuilder();
                for (String arg : tags.get(tag))
                    sb.append("\"" + arg + "\" ");
                sb.setLength(sb.length() - 1);
                return sb.toString();
            }
            return "";
        }

        public List<String> get(String tag) {
            if (tags.containsKey(tag)) return tags.get(tag);
            return null;
        }

        public void remove(String tag) {
            tags.remove(tag);
        }

        public Set<HashMap.Entry<String, List<String>>> entrySet() {
            return tags.entrySet();
        }

        public boolean isMatch(String tag, List<String> args) {
            List<String> targs = get(tag);
            if (targs == null)
                return (args.size() == 0);
            for (int i = 0; i < args.size(); i++) {
                boolean found = false;
                for (int j = 0; j < targs.size() && !found; j++)
                    if (args.get(i).equals(targs.get(j))) found = true;
                if (!found) return false;
            }
            return true;
        }

        public boolean isMatch(String tag, String argstext) {
            List<String> targs = get(tag);
            if (targs == null)
                return (argstext.length() == 0);
            List<String> args = split(argstext);
            for (int i = 0; i < args.size(); i++) {
                boolean found = false;
                for (int j = 0; j < targs.size() && !found; j++)
                    if (args.get(i).equals(targs.get(j))) found = true;
                if (!found) return false;
            }
            return true;
        }

        public Map<String, List<String>> toMap() {
            return tags;
        }

        @Override
        public String toString() {
            if (tags.size() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (HashMap.Entry<String, List<String>> entry : tags.entrySet()) {
                sb.append(entry.getKey() + ":");
                for (String arg : entry.getValue())
                    sb.append(" \"" + arg + '"');
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            return sb.toString();
        }
    }
}