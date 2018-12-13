package info.deskchan.talking_system.speech_exchange;

import info.deskchan.core.Path;
import info.deskchan.talking_system.CharacterController;
import info.deskchan.talking_system.Main;

import java.io.*;

public class SpeechExchanger {

    final CharacterController currentCharacter;

    CharacterSpace<IExchangeable, Reaction> space;

    private DialogLog currentDialog = null;

    public SpeechExchanger(CharacterController character) {
        currentCharacter = character;
        space = new CharacterSpace<>(2);
        setNewDialog();
    }

    public void loadLogs(Path folder){

        for (Path logFile : folder.files()){
            if (logFile.isFile()) {
                try {
                    DialogLog log = new DialogLog(logFile);
                    for (Precedent<IExchangeable, Reaction> row : log.getData()){
                        space.add(row);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    //Main.log(e);
                }
            }
        }
    }

    public void saveCurrentDialog(File file){
        if (currentDialog != null){
            currentDialog.save(file);
        }
    }
    public void setNewDialog(){
        currentDialog = new DialogLog();
    }

    public DialogLine next(){
        currentDialog.add(DialogLog.emptyDialogLine);

        InputData<IExchangeable> data = currentDialog.getInputData(currentDialog.lastIndex());
        CharacterSpace.SearchResult<IExchangeable, Reaction> result = space.getNearest(data);
        if (result.precedent == null){
            result.precedent = new Precedent<IExchangeable, Reaction>(data);
            result.precedent.output = DialogLog.emptyDialogLine.reaction;
        }

        return addAsCharacterSpeech(result.precedent.output);
    }

    public DialogLine next(DialogLine request){
        currentDialog.add(request);

        InputData<IExchangeable> data = currentDialog.getInputData(currentDialog.lastIndex());
        CharacterSpace.SearchResult<IExchangeable, Reaction> result = space.getNearest(data);
        if (result.precedent != null) {
            return addAsCharacterSpeech(result.precedent.output);
        } else {
            return addAsCharacterSpeech(request.reaction);
        }
    }

    public DialogLine addAsCharacterSpeech(Reaction reaction){
        DialogLine newLine = new DialogLine(reaction, currentCharacter, DialogLine.Author.CHARACTER);
        currentDialog.add(newLine);
        return newLine;
    }

    public void deleteUntil(DialogLine deleteTo, Reaction reaction){
        while (currentDialog.get(currentDialog.lastIndex()) != deleteTo){
            currentDialog.remove(currentDialog.lastIndex());
        }
        DialogLine lastLine = currentDialog.get(currentDialog.lastIndex());
        lastLine.reaction = reaction;

        Precedent<IExchangeable, Reaction> p = currentDialog.getPrecedentData(currentDialog.lastIndex());
        space.add(p);
    }

    public String getHistory(){
        StringBuilder sb = new StringBuilder();
        for (DialogLine line : currentDialog) {
            if (line instanceof DialogLog.StartDialogLine){
                sb = new StringBuilder();
            }
            sb.append(Main.getString(line.author.toString().toLowerCase()) + ": [");
            for (int i = 0; i < 8; i++)
                sb.append(line.character[i] + " ");
            sb.append("]\n");

            if (line.reaction.emotion != null)
                sb.append(Main.getString("emotion") + ": " + line.reaction.emotion + "\n");
            sb.append(Main.getString("answer") + ": " + line.reaction.exchangeData.toString() + "\n");
            sb.append("\n");
        }
        return sb.toString();
    }
}
