package info.deskchan.speech_command_system;

import info.deskchan.core_utils.LimitHashMap;

import java.util.*;
import java.util.regex.Pattern;

/** Modified fuzzy regular expression turned into state machine. **/
public class RegularRule{

    /** If you want to interpret rule with order dependence, so 'hello hi' will not be equal to 'hi hello'. **/
    public boolean orderDependent = false;

    /** Hardly decrease matching percent if some words was not found in rule. **/
    public boolean fullMatch = false;

    /** All information contains parse options for single matching operation. **/
    private class ParseOptions{

        /** Last word indexed by parsing, so we know what we should parse next. **/
        public int lastParsedIndex;

        public final boolean orderDependent;

        /** Original text query. **/
        public final String text;

        /** Information which vertex using a word. **/
        public class UsersStats {
            // which word is being used / who is using
            protected ArrayList< Pair<String, HashSet<PhraseLevel>> > users;

            public UsersStats(ArrayList<String> words){
                users = new ArrayList<>();
                for(String word : words)
                    users.add(new Pair<>(word, new HashSet<>()));
            }

            /** Split word. **/
            public void split(int wordIndex, int separatorPos){
                Pair<String, HashSet<PhraseLevel>> element = users.get(wordIndex);
                String left = element.one.substring(0,separatorPos),
                        right = element.one.substring(separatorPos);

                users.add(wordIndex, new Pair<>(right , (HashSet) element.two.clone()));
                users.set(wordIndex, new Pair<>(left  , element.two));
            }
            public void addUser(int index, PhraseLevel user){
                users.get(index).two.add(user);
            }
            public String getWord(int index){
                return users.get(index).one;
            }
            public Set<PhraseLevel> getWordUsers(int wordIndex){
                return users.get(wordIndex).two;
            }
            public int usersCount(int index){
                return users.get(index).two.size();
            }
            public int wordsCount(){
                return users.size();
            }
            public int getWordUsedBy(PhraseLevel level){
                for (int i = 0; i < users.size(); i++)
                    if (users.get(i).two.contains(level)) return i;
                return -1;
            }
        }
        public UsersStats users;
        public ParseOptions(String text, ArrayList<String> words, boolean orderDependent){
            users = new UsersStats(words);
            lastParsedIndex = -1;
            this.orderDependent = orderDependent;
            this.text = text;
        }
    }

    ParseOptions parseOptions;
    private static class SearchResult{
        float result;
        int wordSequenceLength;
        SearchResult(float result, int length){
            this.result = result; wordSequenceLength = length;
        }
    }

    protected abstract class PhraseLevel{
        boolean parsed;
        public boolean required = true;

        public abstract SearchResult parse();
        PhraseLevelComplex parent;
        public abstract boolean canBeRemoved();
        public abstract void remove();
        public abstract int getLastPosition(ParseOptions.UsersStats users);
    }

    protected abstract class PhraseLevelComplex extends PhraseLevel{
        public ArrayList<PhraseLevel> levels = new ArrayList <>();
        public void add(PhraseLevel level){
            levels.add(level);
        }
        public int size(){
            return levels.size();
        }
        public PhraseLevel get(int index){
            return levels.get(index);
        }
        public int getLastPosition(ParseOptions.UsersStats users){
            int position = -1;
            for(PhraseLevel level : levels)
                if(level.getLastPosition(users) > position)
                    position = level.getLastPosition(users);
            return position;
        }
        protected String printer(char separator){
            StringBuilder pr = new StringBuilder();
            if(!required) pr.append("?");
            pr.append("(");
            for(int i=0;i<levels.size();i++) {
                if (i > 0) pr.append(separator);
                pr.append(levels.get(i).toString());
            }

            pr.append(")");
            return pr.toString();
        }
    }

    protected class PhraseLevelTypeAnd extends PhraseLevelComplex{
        public SearchResult parse(){
            int length = 0;
            float currentResult = 0;
            SearchResult[] results = new SearchResult[levels.size()];
            for(int i = 0; i < levels.size(); i++){
                results[i] = levels.get(i).parse();

                if(results[i].result < 0.55 && levels.get(i).required){
                    parsed = false;
                    return new SearchResult(required ? 0f : 0.6f, 1);
                }
                length += results[i].wordSequenceLength;
                currentResult += results[i].wordSequenceLength * results[i].result;
            }

            if (length > 0) currentResult /= length;
            else currentResult = 0;
            parsed = true;
            return new SearchResult(currentResult, length);
        }
        public boolean canBeRemoved(){
            if(!parsed || !required) return true;
            if(parent == null) return false;
            return parent.canBeRemoved();
        }
        public void remove(){
            if(!parsed || parent==null) return;
            parsed=false;
            parent.remove();
        }
        @Override
        public String toString(){ return (required ? "" : "?") + printer(' '); }
    }

    protected class PhraseLevelTypeOr extends PhraseLevelComplex{
        private int found;
        public SearchResult parse(){
            found = 0;
            SearchResult max = new SearchResult(0, 0);
            SearchResult[] results = new SearchResult[levels.size()];
            for(int i=0; i<levels.size(); i++){
                results[i] = levels.get(i).parse();
                if(results[i].result<0.6) continue;
                found++;

                max.result = Math.max(results[i].result, max.result);
                max.wordSequenceLength = Math.max(max.wordSequenceLength, results[i].wordSequenceLength);
            }
            parsed = false;
            if(found == 0) return new SearchResult(0, 0);
            parsed = true;
            return max;
        }
        public boolean canBeRemoved(){
            if(!parsed || !required) return true;
            if(found>1) return true;
            return parent!=null && parent.canBeRemoved();
        }
        public void remove(){
            if(!parsed) return;
            found--;
            if(found>0) return;
            parsed = false;
            parent.remove();
        }
        @Override
        public String toString(){ return (required ? "" : "?")+printer('|'); }
    }

    protected ArrayList<Argument> arguments=new ArrayList<>();

    private enum ArgumentType { Text , Word , List , Integer , Number , Date , Time , DateTime , RelativeDateTime }
    protected class Argument extends PhraseLevel{
        public String name;
        public ArgumentType type;
        private PhraseLevel previous;
        private int lastPos;

        private ArgumentType searchArgument(String search) throws Exception{
            for (ArgumentType each : ArgumentType.class.getEnumConstants())
                if (each.name().compareToIgnoreCase(search) == 0)
                    return each;

            throw new TypeNotPresentException("Unknown argument type: "+search, null);
        }

        public Argument(String text, PhraseLevel previous) throws Exception{
            if(text.charAt(0) != '{')
                throw new Exception("Not argument type: "+text);

            String[] parts = text.substring(1, text.length()-1).split(":");
            this.type = searchArgument(parts[1].toLowerCase());
            this.name = parts[0];
            this.previous = previous;
        }

        public SearchResult parse(){
            dropPosition();
            return new SearchResult(1, 1);
        }

        public void dropPosition(){
            lastPos = -1;
        }

        public Object localize(String text, MatchResult result){
            Object value = null;
            int i = getLastPosition(result.users) + 1;
            Words words = result.words;
            switch(type){
                case Text: {
                    String textCopy = text;
                    for (int k=0; k < words.size(); k++) {
                        if (k < i || words.used[k]) {
                            Pattern pattern = Pattern.compile("(^|[^A-zА-я])("+words.get(k)+")($|[^A-zА-я])");
                            textCopy = pattern.matcher(textCopy).replaceAll("$1$3");
                        }
                        words.used[k] = true;
                    }
                    value = textCopy.trim();
                } break;
                case Word:{
                    for (int k=i; k < words.size(); k++) {
                        if (!words.used[k]) {
                            value = words.get(k);
                            lastPos = k;
                            words.used[k] = true;
                            break;
                        }
                    }
                } break;
                case List: {
                    List<String> list = new ArrayList<>();
                    for (; i < words.size(); i++)
                        if (!words.used[i]){
                            list.add(words.get(i));
                            words.used[i] = true;
                            lastPos = i;
                        }
                    if(list.size()>0) value = list;
                } break;
                case Integer:{
                    Words sub = new Words(words, i, words.size());
                    value = Parsers.parseInteger(sub);
                    lastPos = words.join(sub.used, i);
                } break;
                case Number:{
                    Words sub = new Words(words, i, words.size());
                    value = Parsers.parseNumber(sub);
                    lastPos = words.join(sub.used, i);
                } break;
                case DateTime:{
                    Words sub = new Words(words, i, words.size());
                    value = Parsers.parseDateTime(sub);
                    lastPos = words.join(sub.used, i);
                } break;
                case Time:{
                    Words sub = new Words(words, i, words.size());
                    value = Parsers.parseTime(sub);
                    lastPos = words.join(sub.used, i);
                } break;
                case Date:{
                    Words sub = new Words(words, i, words.size());
                    value = Parsers.parseDate(sub);
                    lastPos = words.join(sub.used, i);
                } break;
                case RelativeDateTime:{
                    Words sub = new Words(words, i, words.size());
                    value = Parsers.parseRelativeDateTime(sub);
                    lastPos = words.join(sub.used, i);
                } break;
            }
            return value;
        }
        public boolean canBeRemoved(){
            return true;
        }
        public void remove(){
            parent.remove();
        }
        public int getLastPosition(ParseOptions.UsersStats users){
            if(lastPos < 0){
                return previous != null ? previous.getLastPosition(users) : 0;
            }
            return lastPos;
        }
        @Override
        public String toString(){ return '{'+name+":"+type.toString()+"}"; }
    }

    /** SIMILAR - we found word that looks similar to required word
     *  SUBWORD - we found required word as the part of other word
     *  DIVIDED - we found some words divided by space that looks like required word as one
     */
    private enum WordType { NONE, SIMILAR, SUBWORD, DIVIDED }

    protected class WordPhrase extends PhraseLevel{
        private String word;
        private int lastPosition;

        private WordPhrase(String word) throws Exception{
            for(int i=0; i<word.length(); i++)
                if(!Character.isLetter(word.charAt(i)))
                    throw new Exception("Wrong word for rule: "+word, null);

            this.word = word;
            parsed = true;
        }

        public SearchResult parse(){
            lastPosition = -1;
            WordType type = WordType.NONE;
            float max = 0;
            int leftBorder = -1, rightBorder = -1,
                wordsCount = parseOptions.users.wordsCount();

            for(int i = parseOptions.lastParsedIndex + 1, k, l = word.length()*2; i < wordsCount; i++){
                String matchingWord = parseOptions.users.getWord(i);

                float result = PhraseComparison.relative(word, matchingWord);
                if(result > 0.99){   // exact match
                    parseOptions.users.addUser(i, this);
                    lastPosition = i;
                    if(parseOptions.orderDependent) parseOptions.lastParsedIndex = i;
                    return new SearchResult(1, word.length());
                }

                if(result > PhraseComparison.ACCURACY && result > max){  // some overlapping, but we do not sure
                    max = result;
                    leftBorder = i;
                    type = WordType.SIMILAR;
                }

                if(matchingWord.length() > word.length()*1.3){   // maybe only start or end of the word matches
                    result = PhraseComparison.relative(matchingWord.substring(0, word.length()), word) * 0.95f;
                    if(result>0.85 && result>max){   // start
                        max=result;
                        leftBorder=i;
                        rightBorder=i;
                        type = WordType.SUBWORD;
                    }
                    result = PhraseComparison.relative(matchingWord.substring(word.length()), word) * 0.95f;
                    if(result>0.85 && result>max){   // end
                        max=result;
                        leftBorder=i;
                        rightBorder=i+1;
                        type = WordType.SUBWORD;
                    }
                }
                StringBuilder p = new StringBuilder(parseOptions.users.getWord(i));
                k = 1;
                while(i+k < wordsCount && p.length() < l) {  // so, maybe a sequence of words matches
                    p.append(parseOptions.users.getWord(i+k));
                    k++;
                    result = PhraseComparison.relative(p.toString(), word);

                    if(max < result){
                        max = result;
                        leftBorder=i;
                        rightBorder=i+k;
                        type = WordType.DIVIDED;
                    }
                }
            }
            if(type == WordType.NONE || max<PhraseComparison.ACCURACY){
                parsed = false;
                return new SearchResult(0, word.length());
            }
            switch(type){
                case SIMILAR:
                    parseOptions.users.addUser(leftBorder, this);
                    return new SearchResult(max, parseOptions.users.getWord(leftBorder).length());
                case SUBWORD:{
                    parseOptions.users.split(leftBorder, word.length());
                    if(parseOptions.orderDependent)
                        parseOptions.lastParsedIndex = rightBorder;
                    parseOptions.users.addUser(rightBorder, this);
                    lastPosition = rightBorder;
                    return new SearchResult(max, word.length());
                }
                case DIVIDED:{
                    StringBuilder p = new StringBuilder();
                    for(int a = leftBorder; a < rightBorder; a++){
                        p.append(parseOptions.users.getWord(a));
                        parseOptions.users.addUser(a, this);
                    }
                    return new SearchResult(max, p.length());
                }
            }
            return new SearchResult(0, word.length());
        }
        public boolean canBeRemoved(){
            return parent.canBeRemoved();
        }
        public int getLastPosition(ParseOptions.UsersStats users){
            return users.getWordUsedBy(this);
        }
        public void remove(){
            parent.remove();
        }
        @Override
        public String toString(){
            return (required ? "" : "?") + word;
        }
    }

    private PhraseLevel start;
    private PhraseLevel last;
    private static String removeBrackets(String phrase) {
        if(phrase.charAt(0)=='(' && phrase.charAt(phrase.length()-1)==')')
            phrase = phrase.substring(1, phrase.length()-1);
        return phrase;
    }
    protected PhraseLevel analyzePhrase(String phrase, PhraseLevelComplex parent) throws Exception{
        int level=0, st=0, blocks=0;

        // removing brackets in case '(ab)' but not in case '(ab) (cd)'
        for(int i=0, len=phrase.length(); i<len; i++){
            if(phrase.charAt(i)=='('){
                if(level == 0) blocks++;
                level++;
            } else if(phrase.charAt(i)==')') level--;
        }
        if(blocks == 1) phrase = removeBrackets(phrase);

        // splitting phrase to elements
        List<String> sequence = new ArrayList<>();
        for(int i=0, len=phrase.length(); i<len; i++){
            if(phrase.charAt(i)==' ' && level==0){
                if(i-st>0) sequence.add(phrase.substring(st, i));
                st = i+1;
            } else if(phrase.charAt(i)=='(') level++;
            else if(phrase.charAt(i)==')') level--;
        }

        // if it's an AND type sequence because of spaces
        if(sequence.size() > 0){
            PhraseLevelTypeAnd levelTypeAnd = new PhraseLevelTypeAnd();
            levelTypeAnd.parent = parent;
            sequence.add(phrase.substring(st));
            for(String element : sequence)
                levelTypeAnd.add(analyzePhrase(element, levelTypeAnd));
            last = levelTypeAnd;
            return levelTypeAnd;
        }

        boolean required = true;
        phrase = phrase.trim();
        if(phrase.charAt(0) == '?'){
            required = false;
            phrase = phrase.substring(1);
        }

        if(blocks == 1) phrase = removeBrackets(phrase);

        // if it's an OR type sequence because of slashes
        if(phrase.indexOf('|') >= 0) {
            PhraseLevelTypeOr phraseLevel = new PhraseLevelTypeOr();
            phraseLevel.parent = parent;
            phraseLevel.required = required;
            st = 0;
            level = 0;
            for (int i = 0, len = phrase.length(); i < len; i++) {
                if (phrase.charAt(i) == '|' && level == 0 && i - st > 0) {
                    phraseLevel.add(analyzePhrase(phrase.substring(st, i), phraseLevel));
                    st = i + 1;
                } else if (phrase.charAt(i) == '(') level++;
                else if (phrase.charAt(i) == ')') level--;
            }
            phraseLevel.add(analyzePhrase(phrase.substring(st), phraseLevel));
            last = phraseLevel;
            return phraseLevel;
        }

        PhraseLevel phraseLevel = null;
        try {
            phraseLevel = new Argument(phrase, last);
            arguments.add((Argument) phraseLevel);
        } catch (Exception e){
            phraseLevel = new WordPhrase(phrase);
        }

        phraseLevel.parent = parent;
        phraseLevel.required = required;
        last = phraseLevel;
        return phraseLevel;
    }
    protected void correctCheck(String rule) throws Exception{
        int state=0,level=0;
        boolean simple_word_present = false;
        for(int i=0, len=rule.length(); i<=len; i++){
            int s=state;
            switch(state){
                case 0:{ // space before word
                    if(i==len) break;
                    state=-1;
                    if(rule.charAt(i)=='?') state=1;
                    else if(rule.charAt(i)=='(') state=2;
                    else if(Character.isLetter(rule.charAt(i))) state=3;
                    else if(rule.charAt(i)==' ') state=0;
                    else if(rule.charAt(i)==')') state=5;
                    else if(rule.charAt(i)=='{') state=6;
                } break;
                case 1:{  // flag before word
                    if(i==len) throw new Exception("Text rule parse error: flag in wrong place, rule=["+rule+"], pos="+i);
                    state=-1;
                    if(rule.charAt(i)=='(') state=2;
                    if(Character.isLetter(rule.charAt(i))) state=3;
                } break;
                case 2:{  // opening bracket
                    level++;
                    if(i==len) throw new Exception("Text rule parse error: open bracket before end of string, rule=["+rule+"]");
                    state=-1;
                    if(rule.charAt(i)=='?') state=1;
                    if(rule.charAt(i)=='(') state=2;
                    if(Character.isLetter(rule.charAt(i))) state=3;
                    if(rule.charAt(i)==' ') state=0;
                    if(rule.charAt(i)=='{') state=6;
                } break;
                case 3:{  // word
                    if(i==len) break;
                    state=-1;
                    simple_word_present = true;
                    if(rule.charAt(i)==' ') state=0;
                    if(Character.isLetter(rule.charAt(i))) state=3;
                    if(rule.charAt(i)=='|') state=4;
                    if(rule.charAt(i)==')') state=5;
                } break;
                case 4:{  // OR operator
                    if(i==len) throw new Exception("Text rule parse error: OR in wrong place, rule=["+rule+"], pos="+i);
                    state=-1;
                    if(rule.charAt(i)=='(') state=2;
                    if(Character.isLetter(rule.charAt(i))) state=3;
                } break;
                case 5:{  // closing bracket
                    level--;
                    if(level<0) throw new Exception("Text rule parse error: excess closing bracket at "+i+", rule=["+rule+"]");
                    if(i==len) break;
                    state=-1;
                    if(rule.charAt(i)==')') state=5;
                    if(rule.charAt(i)==' ') state=0;
                    if(rule.charAt(i)=='|') state=4;
                } break;
                case 6:{  // open brace or first part of argument
                    if(level!=0) throw new Exception("Text rule parse error: argument inside block at "+i+", rule=["+rule+"]");
                    state=-1;
                    if(Character.isLetter(rule.charAt(i))) state = 6;
                    if(rule.charAt(i)==':') state=7;
                } break;
                case 7: {  // second part of argument
                    if(i==len) throw new Exception("Text rule parse error: end of string before closing brace, rule=["+rule+"]");
                    state = -1;
                    if (Character.isLetter(rule.charAt(i))) state = 7;
                    if (rule.charAt(i) == '}') state = 8;
                } break;
                case 8:{
                    if (i == len) break;
                    state = -1;
                    if(rule.charAt(i)==' ') state=0;
                    if(rule.charAt(i)=='|') state=4;
                    if(rule.charAt(i)==')') state=5;
                } break;
            }
            if(state<0) throw new Exception("Text rule parse error: no suitable state to jump next found for state "+s+" at "+i+", rule=["+rule+"], pos="+i);
        }
        if(!simple_word_present) throw new Exception("Text rule parse error: rule should contain at least one word, rule=["+rule+"]");
        if(level!=0) throw new Exception("Text rule parse error: not all brackets closed, level="+level+", rule=["+rule+"]");
    }

    public static RegularRule create(String phrase) throws Exception{
        if (!hash.containsKey(phrase)) {
            hash.put(phrase, new RegularRule(phrase));
            if (debug) System.out.println("created new rule for '" + phrase + "'");
        }
        return hash.get(phrase);
    }

    private String textrule;

    public String getRule(){
        return textrule;
    }

    private RegularRule(String phrase) throws Exception{

        if(phrase.charAt(0)=='!'){
            orderDependent = true;
            phrase = phrase.substring(1);
        }
        correctCheck(phrase);

        StringBuilder sb = new StringBuilder(phrase);
        for(int i=0, in=0; i<phrase.length(); i++){
            if (phrase.charAt(i) == '{') in = 1;
            else if (phrase.charAt(i) == '}') in = 0;
            else if (in == 0) sb.setCharAt(i, Character.toLowerCase(phrase.charAt(i)));
        }

        start = analyzePhrase(sb.toString(), null);
        if(start instanceof WordPhrase || start instanceof Argument) {
            PhraseLevelTypeAnd phraseLevel = new PhraseLevelTypeAnd();
            phraseLevel.add(start);
            start.parent = phraseLevel;
            phraseLevel.parent = null;
            start = phraseLevel;
        }
        start.required = true;
        textrule = start.toString();
    }

    public static class MatchResult{
        final float matchPercentage;
        final Words words;
        final int firstWordUsed;
        final int wordsUsed;
        private final ParseOptions.UsersStats users;
        MatchResult(){
            matchPercentage = 0;
            words = null;
            firstWordUsed = -1;
            wordsUsed = 0;
            users = null;
        }
        MatchResult(MatchResult copy){
            matchPercentage = copy.matchPercentage;
            words = new Words(copy.words);
            firstWordUsed = copy.firstWordUsed;
            wordsUsed = copy.wordsUsed;
            users = copy.users;
        }
        MatchResult(SearchResult result, ParseOptions options){
            matchPercentage = result.result;

            ArrayList<String> wordsList = new ArrayList<>();
            boolean[] used = new boolean[options.users.wordsCount()];
            int fwu = -1, wu = 0;
            for(int i=0; i<used.length; i++){
                wordsList.add(options.users.getWord(i));
                used[i] = (options.users.usersCount(i) > 0);
                if (used[i]){
                    if (fwu<0) fwu = i;
                    wu++;
                }
            }
            words = new Words(wordsList, used);
            firstWordUsed = fwu;
            wordsUsed = wu;
            users = options.users;
        }
        public boolean better(MatchResult other){
            if (matchPercentage < 0.5) return false;
            if (other == null) return true;
            if (wordsUsed != other.wordsUsed) return wordsUsed > other.wordsUsed;
            if (firstWordUsed != other.firstWordUsed)
                return other.firstWordUsed < 0 || firstWordUsed < other.firstWordUsed;
            return matchPercentage >= other.matchPercentage;
        }
        @Override
        public String toString(){
            return "words used: "+wordsUsed+", first word used at: "+firstWordUsed+", match: "+(matchPercentage*100)+"%";
        }
    }

    public MatchResult parse(String phrase, ArrayList<String> words){
        QueryPair query = new QueryPair(this, phrase);
        if (matchingHash.containsKey(query))
            return new MatchResult(matchingHash.get(query));

        if (debug) System.out.println("new parse operation for '" + phrase + "'");
        parseOptions = new ParseOptions(phrase, words, orderDependent);
        SearchResult result = start.parse();

        // whole symbols count in text
        int mass = 0;

        // trying to resolve situations where some words was used more than once, like text 'hi hello' with rule 'hi hello hi'
        for(int i = 0, le; i < words.size(); i++){
            le = parseOptions.users.usersCount(i);
            mass += words.get(i).length();
            if(le > 1){
                Iterator<PhraseLevel> it = parseOptions.users.getWordUsers(i).iterator();
                while (it.hasNext()) {
                    if (it.next().canBeRemoved()) {
                        it.remove();
                        break;
                    }
                }
                // cannot resolve
                if(!start.parsed) {
                    matchingHash.put(query, new MatchResult());
                    return new MatchResult();
                }
            }
        }
        if(fullMatch && result.result > 0.5)
            result.result *= 0.5f + (float) result.wordSequenceLength / mass / 2.0f;

        matchingHash.put(query, new MatchResult(result, parseOptions));
        return new MatchResult(matchingHash.get(query));
    }

    public MatchResult parse(String phrase){
        return parse(phrase, PhraseComparison.toClearWords(phrase));
    }

    public Map<String,Object> getArguments(String text, MatchResult result){
        Map<String, Object> map = new HashMap<>();
        if(arguments.size() == 0)
            return map;

        for(Argument element : arguments)
            element.dropPosition();

        for(Argument element : arguments){
            Object argument = element.localize(text, result);
            if (argument != null)
                map.put(element.name, argument);
        }

        return map;
    }
    @Override
    public String toString(){
        return start.toString();
    }

    private static final LimitHashMap<String, RegularRule> hash = new LimitHashMap<>(100);
    private static final LimitHashMap<QueryPair, MatchResult> matchingHash = new LimitHashMap<>(500);

    private static class QueryPair{
        RegularRule rule;
        String query;
        QueryPair(RegularRule rule, String query){
            this.rule = rule;
            this.query = query;
        }
        @Override
        public boolean equals(Object other){
            if (other.getClass() != getClass()) return false;
            return rule.equals(((QueryPair) other).rule) && query.equals(((QueryPair) other).query);
        }
        @Override
        public int hashCode(){
            return rule.hashCode();
        }
    }

    private static boolean debug = Main.debugBuild;

    static {
        if (debug){
            Testing();
            debug = false;
        }
    }
    public static void Testing(){
        String[] correct_rules = {"погода", "поставь таймер {datetime:RelativeDateTime}", "(открой|запусти) браузер", "включи компьютер", "запусти   будильник {datetime:RelativeDateTime}", "(перейди|открой) ?ярлык (папку|файл)",
                "(посчитай|вычисли|(забей ?в калькулятор)) ?(выражение|пример) {text:Text}", "погода", "рабочий день {start:DateTime} {end:DateTime}", " ?some ?english words", "  ?another   words", " ?anonfm"};
        String[] incorrect_rules = {"(открой||запусти) браузер", "", "(", "??включи компьютер", "?вк?лючи компьютер", "запусти   будильник {date:time:RelativeDateTime}", "(перейди|открой) ?ярлык ((папку|файл)",
                "(посчитай?|вычисли|(забей ?в калькулятор)) ?(выражение|пример) {text:Text}", "{argument:Arg}", "{argument:Integer}"};
        ArrayList<RegularRule> rules = new ArrayList<>();
        for(String text : correct_rules){
            try{
                RegularRule rule = RegularRule.create(text);
                // System.out.println("correct: ["+text+"] / ["+rule.toString()+"]");
                rules.add(rule);
            } catch (Exception e){
                System.out.println("correct: ["+text+"] / [error]");
                System.out.println(e.getMessage());
            }
        }
        for(String text : incorrect_rules){
            try {
                RegularRule rule = RegularRule.create(text);
                System.out.println("incorrect: ["+text+"] | ["+rule.toString()+"]");
                rules.add(rule);
            } catch (Exception e){
                //System.out.println("incorrect: ["+text+"] | [error]");
                //System.out.println(e.getMessage());
            }
        }
        String[] texts = { "поставь таймер на 2 секунды", "поставь таймер пять минут назад", "рабочий день с 10 до 15", "открой мне браузер", "включи кампуктер", "запусти будильник через 5 минут", "забей в калькулятор пример 2+2", "открой ярлык папка", "открой папка", "открой ярлык", "открой перейди файл",
                "браузер будильник запусти", "рабочий день с 10 до 15"};
        for(String text : texts){
            MatchResult bestResult = null;
            RegularRule bestRule = null;
            for(RegularRule rule : rules) {
                MatchResult result = rule.parse(text);
                System.out.println(text+" | "+rule+" | "+result);
                System.out.println(result.words);
                if (result.better(bestResult)){
                    bestResult = result;
                    bestRule = rule;
                }
            }
            if (bestResult != null){
                System.out.println(bestRule.getRule() + " | " + bestRule.getArguments(text, bestResult));
            }
        }
    }
}