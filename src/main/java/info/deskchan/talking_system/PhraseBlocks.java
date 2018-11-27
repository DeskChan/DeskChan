package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PhraseBlocks {

    public static Map<String, BlockFunction> functions = new HashMap<>();

    private List<BlockData> blocks;

    public PhraseBlocks(String input){
        blocks = new LinkedList<>();

        int spos = -1, lastPos = 0, epos = 0;
        while ((spos = findNext(input, lastPos, '}')) >= 0){
            epos = findNext(input, spos + 1, '}');
            if (epos < 0) break;
            lastPos = epos + 1;
            BlockData data = new BlockData(input.substring(spos+1, epos), spos, epos+1);
            blocks.add(data);
        }
    }

    public List<BlockData> getBlocks(){ return blocks; }

    public String replace(String input){
        System.out.println(input);
        System.out.println(blocks);
        for (BlockData entry : blocks){
            System.out.println(entry + " " + functions.containsKey(entry.name));
            if (functions.containsKey(entry.name)) {
                try {
                    input = functions.get(entry.name).insert(input, entry);
                } catch (Exception e) {
                    Main.log("Cannot replace block " + entry + " in phrase: " + input);
                    Main.log(e);
                }
            }
        }
        return input;
    }

    static class BlockData {
        final int start;
        final int end;
        final String name;
        final String[] args;

        BlockData(String input, int start, int end){
            this.start = start;
            this.end = end;

            String[] parts = input.trim().split("\\(", 2);
            name = parts[0];
            if (parts.length == 1) {
                this.args = null;
                return;
            }

            parts = parts[1].split("\\s*,\\s*");
            List<String> args = new LinkedList<>();
            for (String part : parts){
                part = part.trim();
                if (part.endsWith(")")){
                    if (part.length() == 1)
                        break;
                    else
                        part = part.substring(0, part.length() - 1);
                }
                if (part.startsWith("\"") && part.endsWith("\""))
                    part = part.substring(1, part.length()-1);
                if (part.startsWith("'") && part.endsWith("'"))
                    part = part.substring(1, part.length()-1);
                args.add(part);
            }
            if (args.size() > 0)
                this.args = args.toArray(new String[args.size()]);
            else
                this.args = null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{"); sb.append(name);
            if (args != null){
                sb.append("(");
                sb.append(args[0]);
                for (int i = 1; i < args.length; i++) {
                    sb.append(",");
                    sb.append(args[i]);
                }
                sb.append(")");
            }
            sb.append("}");
            return sb.toString();
        }

        public String standardReplace(String input, String insert){
            return input.substring(0, start) + insert + input.substring(end);
        }
    }

    interface BlockFunction {
        String insert(String input, BlockData data);
    }

    protected static int findNext(String input, int start, Character endSymbol){
        LinkedList<Character> stack = new LinkedList<>();
        stack.add(endSymbol);
        char inQuote = 0;
        for (int j = start; j < input.length(); j++) {
            Character c = input.charAt(j);

            if (c == '(') c = ')';
            else if (c == '[') c = ']';
            else if (c == '{') c = '}';

            if (inQuote != 0){
                if (c == inQuote)
                    inQuote = 0;
            } else if (c == '"' || c == '\''){
                inQuote = c;
            } else if (c == ')' || c == ']' || c == '}'){
                if (stack.getLast() == c)
                    stack.removeLast();
                else
                    stack.addLast(c);
            }
            if (inQuote == 0)
                if ((stack.size() == 0 && c == endSymbol) || (stack.size() == 1 && stack.get(0) == c))
                    return j;
        }
        return -1;
    }

    /*  --- Default replace handlers --- */

    static void initialize() {
        functions.put("userF", userF);
        functions.put("user", user);
        functions.put("name", name);
        functions.put("tag", tag);
        functions.put("date", date);
        functions.put("time", time);
        functions.put("randomcount", randomcount);
        functions.put("abuse", abuse);
        functions.put("weekday", weekday);
        functions.put("year", year);
    }

    private static BlockFunction name = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            String name = (Main.getCurrentCharacter() != null && Main.getCurrentCharacter().name != null) ?
                    Main.getCurrentCharacter().name :
                    Main.getString("default_name");
            return input.substring(0, data.start) + name + input.substring(data.end);
        }
    };

    private static BlockFunction user = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            List<List<String>> text = TextOperations.splitSentence(input);

            String name = null;
            if (Main.getCurrentCharacter() != null && Main.getCurrentCharacter().tags != null)
                name = Main.getCurrentCharacter().tags.getRandom("usernames");
            if (name == null)
                name = Main.getString("default_username");

            for (List<String> sentence : text){
                int index = sentence.indexOf("{user}");
                if (index < 0) continue;

                if (sentence.size() < 3){
                    sentence.set(index, name);
                    continue;
                }
                sentence.remove(index);
                if (index > 0) {
                    sentence.remove(index - 1);
                } else {
                    sentence.remove(index);  // was index + 1 before removal
                }

                int r = new Random().nextInt(sentence.size() / 2);
                sentence.add(r * 2, name);
                if (r == 0){
                    sentence.add(r * 2 + 1, ",");
                } else {
                    sentence.add(r * 2, ",");
                }
            }

            StringBuilder sb = new StringBuilder();
            for (List<String> sentence : text){
                for (int i = 0; i < sentence.size(); i+=2){
                    sb.append(sentence.get(i));
                    sb.append(sentence.get(i+1));
                    sb.append(" ");
                }
            }
            return sb.toString();
        }
    };

    private static BlockFunction userF = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            String name = null;
            if (Main.getCurrentCharacter() != null && Main.getCurrentCharacter().tags != null)
                name = Main.getCurrentCharacter().tags.getRandom("usernames");
            if (name == null)
                name = Main.getString("default_username");

            return data.standardReplace(input, name);
        }
    };

    private static BlockFunction abuse = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            String name = null;
            if (Main.getCurrentCharacter() != null && Main.getCurrentCharacter().tags != null)
                name = Main.getCurrentCharacter().tags.getRandom("abuses");
            if (name == null)
                name = Main.getString("default_abuse");

            return data.standardReplace(input, name);
        }
    };

    private static BlockFunction tag = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            String tag = Main.getCurrentCharacter() != null && Main.getCurrentCharacter().tags != null ?
                    Main.getCurrentCharacter().tags.getRandom(data.args[0]) : null;
            return data.standardReplace(input, tag != null ? tag : "%none%");
        }
    };

    private static BlockFunction date = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            DateFormat formatter = new SimpleDateFormat("dd LLLL");
            return data.standardReplace(input, formatter.format(new Date()));
        }
    };

    private static BlockFunction time = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            DateFormat formatter = new SimpleDateFormat("HH:mm");
            return data.standardReplace(input, formatter.format(new Date()));
        }
    };

    private static BlockFunction year = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            DateFormat formatter = new SimpleDateFormat("YYYY");
            return data.standardReplace(input, formatter.format(new Date()));
        }
    };

    private static BlockFunction weekday = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            DateFormat formatter = new SimpleDateFormat("EEEE");
            return data.standardReplace(input, formatter.format(new Date()));
        }
    };

    private static BlockFunction randomcount = new BlockFunction() {
        @Override
        public String insert(String input, BlockData data) {
            String repeat = data.args[0];
            int count = Integer.parseInt(data.args[1]);
            StringBuilder sb = new StringBuilder();
            sb.append(repeat.substring(0, data.start));
            for (int i = 0; i < count; i++)
                sb.append(repeat);
            sb.append(input.substring(data.end));
            return sb.toString();
        }
    };
}
