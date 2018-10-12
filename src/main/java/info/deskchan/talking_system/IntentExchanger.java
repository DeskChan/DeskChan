package info.deskchan.talking_system;

import info.deskchan.core.Path;
import info.deskchan.talking_system.intent_exchange.*;

import java.io.*;
import java.util.*;

public class IntentExchanger {

    List<String> bannedIntents = Arrays.asList("SET_TOPIC", "WHAT_YOUR_OPINION", "QUESTION");


    CharacterController currentCharacter;

    CharacterSpace<IntentList, Reaction> space;

    private DialogLog currentDialog = null;

    public IntentExchanger(CharacterController character) {
        currentCharacter = character;
        generate();
        setNewDialog();
    }

    private void generate(){

        Path folder = Main.getDialogLogsDirPath();

        space = new CharacterSpace<>(2);

        for (Path logFile : folder.files()){
            if (logFile.isFile()) {
                try {
                    DialogLog log = new DialogLog(logFile);
                    for (Precedent<IntentList, Reaction> row : log.getData()){
                        space.add(row);
                    }
                } catch (Exception e){
                    Main.log(e);
                }
            }
        }
        Main.log("Intent exchanger loading completed");
    }

    public void saveCurrentDialog(){
        if (currentDialog != null){
            currentDialog.save();
        }
    }
    public void setNewDialog(){
        currentDialog = new DialogLog();
        addByCharacter(null);
    }

    public DialogLine next(){
        currentDialog.add(DialogLine.EMPTY);

        InputData<IntentList> data = currentDialog.getData(currentDialog.lastIndex());
        CharacterSpace.SearchResult<IntentList, Reaction> result = space.getNearest(data);

        return addByCharacter(result.precedent.output);
    }

    public DialogLine next(DialogLine request){
        currentDialog.add(request);

        InputData<IntentList> data = currentDialog.getData(currentDialog.lastIndex());
        CharacterSpace.SearchResult<IntentList, Reaction> result = space.getNearest(data);

        deleteBannedIntents(result.precedent.output);
        if (result.precedent.output.intents.size() == 0){
            result.precedent.output.intents.add("NOTHING");
        }

        return addByCharacter(result.precedent.output);
    }

    private void deleteBannedIntents(Reaction reaction){
        for (int i = 0; i < reaction.intents.size(); i++) {
            for (String ban : bannedIntents) {
                if (reaction.intents.get(i).contains(ban)) {
                    reaction.intents.remove(i);
                    i--;
                }
            }
        }
    }

    public DialogLine addByCharacter(Reaction reaction){
        DialogLine newLine = new DialogLine(reaction, currentCharacter, true);
        currentDialog.add(newLine);
        return newLine;
    }

    public void deleteUntil(DialogLine deleteTo, Reaction reaction){
        while (currentDialog.get(currentDialog.lastIndex()) != deleteTo){
            currentDialog.remove(currentDialog.lastIndex());
        }
        DialogLine lastLine = currentDialog.get(currentDialog.lastIndex());
        lastLine.reaction = reaction;

        Precedent<IntentList, Reaction> p = new Precedent<>(currentDialog.getData(currentDialog.lastIndex()));
        p.output = reaction;
        space.add(p);
    }

    public String getHistory(){
        return currentDialog.toString();
    }

    public static class DialogLine {
        float[] character;
        Reaction reaction;
        public DialogLine(){
            character = CharacterController.asArray(new StandardCharacterController());
            reaction = new Reaction();
            isStart = true;
        }
        public DialogLine(Reaction reaction, CharacterController character, boolean isCharacter){
            this(reaction, CharacterController.asArray(character), isCharacter);
        }
        public DialogLine(Reaction reaction, float[] character, boolean isCharacter){
            this.character = character;
            this.reaction = reaction;
            this.isCharacter = isCharacter;
        }
        boolean isCharacter = false;
        boolean isStart = false;

        static final DialogLine EMPTY = new DialogLine();
    }

    public static class Reaction implements ICompatible {
        IntentList intents = new IntentList();
        String emotion = null;
        String originText;

        public Reaction(){}
        public Reaction(IntentList intents){
            this.intents = intents;
        }
        public Reaction(IntentList intents, String emotion){
            this.intents = intents;
            this.emotion = emotion;
        }
        @Override
        public double checkCompatibility(ICompatible other) {
            if (!(other instanceof Reaction))
                return 0;

            Reaction o = (Reaction) other;
            return (intents.checkCompatibility(o.intents) + (emotion.equals(o.emotion) ? 1 : 0)) / 2;
        }

        @Override
        public String toString() {
            return "Intents: " + intents + ", Emotion:" + (emotion != null ? emotion : " - ");
        }
    }

    public static class DialogLog extends LinkedList<DialogLine> {

        public DialogLog(File file) throws Exception {
            BufferedReader writer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            DialogLine replica = null;
            boolean first = DialogLine.EMPTY.isCharacter, toggleNewDialog = true;

            add(DialogLine.EMPTY);

            while ((line = writer.readLine()) != null){
                line = line.trim();
                if (line.length() == 0) continue;
                if (line.matches("\\#+")) {
                    toggleNewDialog = true;
                    continue;
                }
                if (line.matches("[\\-0-9\\s]+")){
                    replica = new DialogLine();
                    if (toggleNewDialog){
                        replica.isStart = true;
                        toggleNewDialog = false;
                    }
                    String[] n = line.split(" ");
                    for (int i = 0; i < 8; i++)
                        replica.character[i] = Integer.parseInt(n[i]);
                    replica.isCharacter = first;
                    first = !first;
                    add(replica);
                } else if (replica != null){
                    if (line.startsWith(">"))
                        replica.reaction.emotion = line.substring(1).trim();
                    else
                        replica.reaction.intents.add(line);
                }
            }
            writer.close();
        }

        public DialogLog() { }

        public List<Precedent<IntentList, Reaction>> getData(){
            List<Precedent<IntentList, Reaction>> set = new LinkedList<>();
            for (int i = 0; i < size()-1; i++){
                if (get(i).reaction == null) continue;
                InputData<IntentList> data = getData(i);
                Precedent<IntentList, Reaction> p = new Precedent<>(data);
                p.output = get(i+1).reaction;
                set.add(p);
            }

            return set;
        }

        public static final int HISTORY_CAPACITY = 3;
        public InputData<IntentList> getData(int index){
            DialogLine Y = get(index), cur;
            List<float[]> first = new ArrayList<>(), second = new ArrayList<>();
            List<IntentList> history = new ArrayList<>();
            first.add(Y.character);

            if (!Y.isStart)
            for (int j = index-1; j >= 0 && first.size() < HISTORY_CAPACITY && second.size() < HISTORY_CAPACITY; j--){
                cur = get(j);
                if (cur.isCharacter == Y.isCharacter){
                    first.add(cur.character);
                } else {
                    second.add(cur.character);
                }
                if (cur.reaction != null)
                    history.add(cur.reaction.intents);
                if (cur.isStart) break;
            }

            InputData<IntentList> precedent = new InputData<>(
                    history.toArray(new IntentList[history.size()]),
                    get(index).reaction.intents
            );
            precedent.setCoords(first, second);
            return precedent;
        }

        public void save() {
            if (size() == 0) return;
            try {
                Path file = Main.getDialogLogsDirPath().resolve("log-" + new Date().getTime() + ".txt");
                if (!file.exists())
                    file.createNewFile();
                BufferedWriter writer = file.newBufferedWriter();
                writer.write(toString());
                writer.flush();
                writer.close();
            } catch (Exception e) {
                Main.log(e);
            }
        }

        public int lastIndex(){ return size() - 1; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (DialogLine line : this) {
                if (line.reaction == null || line.reaction.intents.size() == 0) continue;
                if (line.isStart){
                    sb.append("################\n");
                }
                for (int i = 0; i < 8; i++)
                    sb.append(line.character[i] + " ");
                sb.append("\n");
                if (line.reaction.emotion != null)
                    sb.append("> " + line.reaction.emotion + "\n");
                for (String intent : line.reaction.intents)
                    sb.append(intent + "\n");
            }
            return sb.toString();
        }
    }
}
