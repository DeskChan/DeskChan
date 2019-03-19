package info.deskchan.talking_system.speech_exchange;

import info.deskchan.talking_system.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DialogLog extends LinkedList<DialogLine> {

    private enum Exchangeable {
        INTENT_LIST("~"), PHRASE(">"), EMOTION("!");

        public String start;
        Exchangeable(String item){ start = item; }
    }

    public static final int HISTORY_CAPACITY = 3;

    public DialogLog() {}

    public DialogLog(File file) throws Exception {
        BufferedReader writer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        DialogLine replica = null;
        DialogLine.Author author = DialogLine.Author.CHARACTER;
        boolean dialogStart = true;

        while ((line = writer.readLine()) != null){
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.matches("\\#+")) {
                dialogStart = true;
                continue;
            }

            if (line.matches("[\\-0-9\\s]+")){
                if (dialogStart){
                    replica = new StartDialogLine();
                    dialogStart = false;
                } else {
                    replica = new DialogLine();
                }

                String[] n = line.split(" ");
                for (int i = 0; i < 8; i++)
                    replica.character[i] = Integer.parseInt(n[i]);

                replica.author = author;

                author = author.next();
                add(replica);

            } else if (replica != null){
                if (replica.reaction == null) replica.reaction = new Reaction();

                if (line.startsWith(Exchangeable.EMOTION.start))
                    replica.reaction.emotion = line.substring(1).trim();
                else if (line.startsWith(Exchangeable.INTENT_LIST.start))
                    replica.reaction.exchangeData = new IntentsData(line.substring(1).trim());
                else if (line.startsWith(Exchangeable.PHRASE.start))
                    replica.reaction.exchangeData = new PhraseData(line.substring(1).trim());
            }
        }
        writer.close();
    }

    public List<Precedent<IExchangeable, Reaction>> getData(){
        List<Precedent<IExchangeable, Reaction>> set = new LinkedList<>();
        for (int i = 0; i < size(); i++){
            set.add(getPrecedentData(i));
        }

        return set;
    }

    public InputData<IExchangeable> getInputData(int index){
        DialogLine end = get(index), cur;
        List<float[]> first = new ArrayList<>(), second = new ArrayList<>();
        List<IExchangeable> history = new ArrayList<>();
        first.add(end.character);

        if (!(end instanceof StartDialogLine))
            for (int j = index-1; j >= 0 && first.size() < HISTORY_CAPACITY && second.size() < HISTORY_CAPACITY; j--){
                cur = get(j);
                if (cur.author == end.author){
                    first.add(cur.character);
                } else {
                    second.add(cur.character);
                }
                if (cur.reaction != null)
                    history.add(cur.reaction.exchangeData);
                if (cur instanceof StartDialogLine) break;
            }

        InputData<IExchangeable> input = new InputData<>(
                history.toArray(new IExchangeable[history.size()]),
                end.reaction != null ? end.reaction.exchangeData : null
        );
        input.setCoords(first, second);
        return input;
    }

    public Precedent<IExchangeable, Reaction> getPrecedentData(int index){
        DialogLine reaction = get(index), cur;
        List<float[]> first = new ArrayList<>(), second = new ArrayList<>();
        List<IExchangeable> history = new ArrayList<>();
        second.add(reaction.character);

        Precedent<IExchangeable, Reaction> precedent = new Precedent<>();
        precedent.output = reaction.reaction;

        if (!(reaction instanceof StartDialogLine) && index > 0) {
            DialogLine input = get(index-1);
            precedent.input = input.reaction != null ? input.reaction.exchangeData : null;
            first.add(input.character);

            if (!(input instanceof StartDialogLine))
            for (int j = index - 2; j >= 0 && first.size() < HISTORY_CAPACITY && second.size() < HISTORY_CAPACITY; j--) {
                cur = get(j);
                if (cur.author == reaction.author) {
                    second.add(cur.character);
                } else {
                    first.add(cur.character);
                }
                if (cur.reaction != null)
                    history.add(cur.reaction.exchangeData);
                if (cur instanceof StartDialogLine) break;
            }
        }

        precedent.history = history.toArray(new IExchangeable[history.size()]);
        precedent.setCoords(first, second);
        return precedent;
    }

    public void save(File file) {
        if (size() == 0) return;
        try {
            if (!file.exists())
                file.createNewFile();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
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
            if (line instanceof StartDialogLine){
                sb.append("\n################\n\n");
            }
            for (int i = 0; i < 8; i++)
                sb.append(line.character[i] + " ");
            sb.append("\n");

            if (line.reaction.emotion != null)
                sb.append(Exchangeable.EMOTION.start + line.reaction.emotion + "\n");
            if (line.reaction.exchangeData instanceof PhraseData)
                sb.append(Exchangeable.PHRASE.start);
            else if (line.reaction.exchangeData instanceof IntentsData)
                sb.append(Exchangeable.INTENT_LIST.start);
            sb.append(line.reaction.exchangeData.toString() + "\n");
        }
        return sb.toString();
    }

    protected static class StartDialogLine extends DialogLine { }

    protected static final DialogLine emptyDialogLine = new DialogLine();
}
