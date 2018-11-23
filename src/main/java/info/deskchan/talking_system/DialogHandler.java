package info.deskchan.talking_system;

import info.deskchan.MessageData.DeskChan.ShowTechnical;
import info.deskchan.MessageData.GUI.Control;
import info.deskchan.MessageData.GUI.InlineControls;
import info.deskchan.MessageData.GUI.SetPanel;
import info.deskchan.core.MessageDataMap;
import info.deskchan.core.MessageListener;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core_utils.TextOperations;
import info.deskchan.talking_system.speech_exchange.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DialogHandler {

    static SpeechExchanger speechExchanger;
    static CharacterClassifier characterClassifier;
    static IntentClassifier intentClassifier;

    static PluginProxyInterface pluginProxy;

    private static IntentList allIntents;

    private static CharacterRange lastRange;
    private static IntentList lastIntents;

    private static String inputPhraseText = "";
    private static String inputIntents = "";
    private static String inputIntentSelected = "";
    private static String output = "";
    private static String outputIntentSelected = "";
    private static String inputEmotion = null;
    private static String outputEmotion = null;
    private static List<String> emotionsList = null;
    private static DialogLine lastRequest = null;
    private static DialogLine lastResponse = null;
    
    private static List<String> bannedIntents = Arrays.asList("SET_TOPIC", "WHAT_YOUR_OPINION", "QUESTION");

    public static void initialize(){

        pluginProxy = Main.getPluginProxy();

        updateDialogModules();
        speechExchanger = new SpeechExchanger(Main.getCurrentCharacter().character);
        speechExchanger.loadLogs(Main.getDialogLogsDirPath());
        Main.log("Intent exchanger loading completed");

        pluginProxy.setAlternative("DeskChan:user-said", "talk:classify-text", 50000);
        pluginProxy.setAlternative("DeskChan:user-said", "talk:dialog-receive", 25);

        pluginProxy.addMessageListener("talk:classify-text", (sender, tag, data) -> {
            MessageDataMap map = new MessageDataMap("value", data);
            IntentList intents = intentClassifier.classify(map.getString("value"));
            lastRange = characterClassifier.getCharacterForPhrase(map.getString("value"));
            lastIntents = new IntentList(intents);
            map.put("intent", intents);
            map.put("character", lastRange.toMap());

            pluginProxy.callNextAlternative(sender, "DeskChan:user-said", "talk:classify-text", map);
        });

        pluginProxy.addMessageListener("talk:character-updated", (sender, tag, data) -> {
            updateDialogModules();
        });

        pluginProxy.addMessageListener("talk:dialog-receive", (sender, tag, data) -> {

            MessageDataMap map = new MessageDataMap(data);

            inputPhraseText = TextOperations.prettifyText(map.getString("value"));
            if (!map.containsKey("intent")) {
                lastIntents = new IntentList(intentClassifier.classify(map.getString("value")));
            } else {
                lastIntents = new IntentList((List<String>) map.get("intent"));
            }

            if (!map.containsKey("character")) {
                lastRange = characterClassifier.getCharacterForPhrase(map.getString("value"));
            } else {
                lastRange = new CharacterRange((Map) map.get("character"));
            }

            inputIntents = lastIntents.toString();
            if (lastIntents.size() > 0){
                lastRequest = new DialogLine(
                        new Reaction(lastIntents, inputEmotion),
                        lastRange.toCentersArray(),
                        DialogLine.Author.USER
                );
            } else {
                lastRequest = new DialogLine(
                        new Reaction(inputPhraseText, inputEmotion),
                        lastRange.toCentersArray(),
                        DialogLine.Author.USER
                );
            }

            DialogLine answer = speechExchanger.next(lastRequest);

            outputEmotion = answer.getReaction().getEmotion();
            if (outputEmotion != null)
                Main.getCurrentCharacter().emotionState.raiseEmotion(outputEmotion);

            if (answer.getReaction().getExchangeData() instanceof PhraseData){
                PhraseData phrase = (PhraseData) answer.getReaction().getExchangeData();
                output = phrase.toString();
            } else if (answer.getReaction().getExchangeData() instanceof IntentsData){
                IntentList list = (IntentList) answer.getReaction().getExchangeData();
                list.removeAll(bannedIntents);
                if (list.size() == 0){
                    list.add("NOTHING");
                }
                for (String intent : list)
                    Main.phraseRequest(intent);
                output = list.toString();
            } else {
                throw new RuntimeException("There is a bug in dialog handler.");
            }

            lastResponse = answer;
            inputEmotion = null;

            setInputInForm();
            setOutputInForm();
            Main.getInstance().resetTimer();
        });

        pluginProxy.addMessageListener("dialog-debug:input-text-changed", (sender, tag, data) -> {
            inputPhraseText = (String) data;
        });

        for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
            String feature = CharacterRange.getFeatureName(i);
            pluginProxy.addMessageListener("dialog-debug:set-input-character-"+feature+"-start", inputCharacterChanged);
            pluginProxy.addMessageListener("dialog-debug:set-input-character-"+feature+"-end",   inputCharacterChanged);
        }

        pluginProxy.addMessageListener("dialog-debug:input-exchangeData-changed", (sender, tag, data) -> {
            inputIntents = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:input-exchangeData-selected", (sender, tag, data) -> {
            inputIntentSelected = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:input-exchangeData-clicked", (sender, tag, data) -> {
            IntentList in = new IntentList(inputIntents);
            in.add(inputIntentSelected);
            inputIntents = in.toString();
            setInputInForm();
        });

        pluginProxy.addMessageListener("dialog-debug:save-input", (sender, tag, data) -> {
            if (inputIntents.trim().length() > 0){
                Phrase newPhrase = new Phrase(inputPhraseText);
                newPhrase.character = lastRange;
                newPhrase.setIntents(new IntentList(inputIntents));

                lastRequest.setReaction(new Reaction(
                        newPhrase.intentType,
                        inputEmotion
                ));

                PhrasesPack pack = Main.getCurrentCharacter().phrases.getUserDatabasePack();
                pack.add(newPhrase);
                pack.save();

                intentClassifier.add(newPhrase);
                characterClassifier.add(newPhrase);
            } else {
                lastRequest.setReaction(new Reaction(
                        inputPhraseText,
                        inputEmotion
                ));
            }
        });

        pluginProxy.addMessageListener("dialog-debug:output-exchangeData-changed", (sender, tag, data) -> {
            output = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:output-exchangeData-selected", (sender, tag, data) -> {
            outputIntentSelected = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:output-exchangeData-clicked", (sender, tag, data) -> {
            IntentList in = new IntentList(output);
            in.add(outputIntentSelected);
            output = in.toString();
            setOutputInForm();
        });

        pluginProxy.addMessageListener("dialog-debug:input-emotion-changed", (sender, tag, data) -> {
            inputEmotion = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:output-emotion-changed", (sender, tag, data) -> {
            outputEmotion = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:apply-output-emotion", (sender, tag, data) -> {
            lastResponse.setReaction(new Reaction(lastResponse.getReaction().getExchangeData(), outputEmotion));
        });

        pluginProxy.addMessageListener("dialog-debug:show-history", (sender, tag, data) -> {
            String history = speechExchanger.getHistory();
            if (history.length() == 0)
                history = Main.getString("empty");
            pluginProxy.sendMessage(new ShowTechnical(
                    history,
                    Main.getString("dialog-debug.history")
            ));
        });

        pluginProxy.addMessageListener("dialog-debug:output-type-changed", (sender, tag, data) -> {
            boolean disabled = !data.equals("intents");
            pluginProxy.sendMessage(new SetPanel(
                    "dialog-debug",
                    SetPanel.PanelType.PANEL,
                    SetPanel.ActionType.UPDATE,
                    new Control(
                            Control.ControlType.ComboBox,
                            "output-intent-list", null,
                            "disabled", disabled
                    ),
                    new Control(
                            Control.ControlType.Button,
                            "output-intent-add-button", null,
                            "disabled", disabled
                    )
            ));
        });


        pluginProxy.addMessageListener("dialog-debug:save-output", (sender, tag, data) -> {
            IntentList output = new IntentList(DialogHandler.output);
            speechExchanger.deleteUntil(lastResponse, new Reaction(output, outputEmotion));

            Main.phraseRequest("CORRECTED");

            for (String intent : output)
                Main.phraseRequest(intent);
        });

        pluginProxy.addMessageListener("dialog-debug:save-dialog-log", (sender, tag, data) -> {
            speechExchanger.saveCurrentDialog(Main.getDialogLogsDirPath().resolve("log-" + new Date().getTime() + ".txt"));
        });

    }

    private static MessageListener inputCharacterChanged = new MessageListener() {
        @Override public void handleMessage(String sender, String tag, Object data) {
            String[] parts = tag.split("-");
            String feature = parts[4];
            Range range = lastRange.range[CharacterRange.getFeatureIndex(feature)];
            if (parts[5].equals("start"))
                range = new Range((Integer) data, range.end);
            else
                range = new Range(range.start, (Integer) data);
            lastRange.range[CharacterRange.getFeatureIndex(feature)] = range;

            setInputInForm();
        }
    };
    public static void resetPanel(){
        emotionsList = Main.getCurrentCharacter().emotionState.getEmotionsList(); emotionsList.add(0, "");

        List<Control> controls = new LinkedList<>();
        controls.addAll(Arrays.asList(
                new Control(
                        Control.ControlType.Button, null,
                        Main.getString("dialog-debug.show-history"),
                        "msgTag", "dialog-debug:show-history"
                ),
                new Control(
                        Control.ControlType.TextField,
                        "phraseText",
                        null,
                        "onChangeTag", "dialog-debug:input-text-changed"
                ),
                new Control(
                        Control.ControlType.Label, null,
                        Main.getString("dialog-debug.phrase-character-diapason")
                )
        ));
        for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
            String feature = CharacterFeatures.getFeatureName(i);
            InlineControls row = new InlineControls();
            row.add(new Control(
                    Control.ControlType.Spinner,
                    "input-character-"+feature+"-start",
                    0,
                    "min", -CharacterFeatures.BORDER,
                    "max",  CharacterFeatures.BORDER,
                    "step", 1,
                    "msgTag", "dialog-debug:set-input-character-"+feature+"-start"
            ));
            row.add(new Control(
                    Control.ControlType.Spinner,
                    "input-character-"+feature+"-end",
                    0,
                    "min", -CharacterFeatures.BORDER,
                    "max",  CharacterFeatures.BORDER,
                    "step", 1,
                    "msgTag", "dialog-debug:set-input-character-"+feature+"-end"
            ));
            row.setLabel(Main.getString(CharacterFeatures.getFeatureName(i)));
            row.setHint(Main.getString("help."+ CharacterFeatures.getFeatureName(i)));
            controls.add(row);
        }
        controls.addAll(Arrays.asList(
                new Control(
                        Control.ControlType.Label, null,
                        Main.getString("dialog-debug.phrase-intents")
                ),
                new InlineControls(
                        new Control(
                                Control.ControlType.TextField,
                                "input-exchangeData-text",
                                null,
                                "onChangeTag", "dialog-debug:input-exchangeData-changed"
                        ),
                        new Control(
                                Control.ControlType.ComboBox,
                                "input-intent-list",
                                null,
                                "values", allIntents,
                                "msgTag", "dialog-debug:input-exchangeData-selected"
                        ),
                        new Control(
                                Control.ControlType.Button,
                                "input-intent-add-button",
                                Main.getString("add"),
                                "msgTag", "dialog-debug:input-exchangeData-clicked"
                        )
                ),
                new Control(
                        Control.ControlType.Button,
                        null,
                        Main.getString("save"),
                        "msgTag", "dialog-debug:save-input"
                ),

                new Control(Control.ControlType.Separator),
                new Control(
                        Control.ControlType.Label, null,
                        Main.getString("dialog-debug.answer-options")
                ),
                new Control(
                        Control.ControlType.ComboBox,
                        "output-type",
                        null,
                        "label", Main.getString("dialog-debug.output-type"),
                        "values", Arrays.asList("intents", "phrase"),
                        "valuesNames", Arrays.asList(Main.getString("intents"), Main.getString("phrase")),
                        "msgTag", "dialog-debug:output-type-changed"
                ),
                new InlineControls(
                        new Control(
                                Control.ControlType.TextField,
                                "output-exchangeData-text",
                                null,
                                "onChangeTag", "dialog-debug:output-exchangeData-changed"
                        ),
                        new Control(
                                Control.ControlType.ComboBox,
                                "output-intent-list",
                                null,
                                "values", allIntents,
                                "msgTag", "dialog-debug:output-exchangeData-selected"
                        ),
                        new Control(
                                Control.ControlType.Button,
                                "output-intent-add-button",
                                Main.getString("add"),
                                "msgTag", "dialog-debug:output-exchangeData-clicked"
                        )
                ),

                new Control(
                        Control.ControlType.Button,
                        null,
                        Main.getString("save"),
                        "msgTag", "dialog-debug:save-output"
                ),

                new Control(Control.ControlType.Separator),
                new InlineControls(
                        "input-emotion-trigger-line",
                        Main.getString("dialog-debug.input-emotion-triggers"),
                        Main.getString("dialog-debug.input-emotion-hint"),
                        new Control(
                                Control.ControlType.ComboBox,
                                "input-emotion-trigger",
                                null,
                                "values", emotionsList,
                                "msgTag", "dialog-debug:input-emotion-changed"
                        ),
                        new Control(
                                Control.ControlType.Button, null,
                                Main.getString("choose"),
                                "msgTag", "dialog-debug:apply-input-emotion"
                        )
                ),
                new InlineControls(
                        "output-emotion-trigger-line",
                        Main.getString("dialog-debug.output-emotion-triggers"),
                        Main.getString("dialog-debug.output-emotion-hint"),
                        new Control(
                                Control.ControlType.ComboBox,
                                "output-emotion-trigger",
                                null,
                                "values", emotionsList,
                                "msgTag", "dialog-debug:output-emotion-changed"
                        ),
                        new Control(
                                Control.ControlType.Button, null,
                                Main.getString("choose"),
                                "msgTag", "dialog-debug:apply-output-emotion"
                        )
                ),
                new Control(Control.ControlType.Separator),
                new Control(
                        Control.ControlType.Button,
                        null,
                        Main.getString("dialog-debug.save-dialog"),
                        "msgTag", "dialog-debug:save-dialog-log"
                )
        ));
        pluginProxy.sendMessage(new SetPanel(
                "dialog-debug",
                SetPanel.PanelType.PANEL,
                SetPanel.ActionType.SET,
                Main.getString("dialog-debug"),
                null,
                null,
                controls
        ));
    }

    public static void setInputInForm(){
        SetPanel update = new SetPanel(
                "dialog-debug",
                SetPanel.PanelType.PANEL,
                SetPanel.ActionType.UPDATE,

                new Control(Control.ControlType.TextField, "phraseText", inputPhraseText),
                new Control(Control.ControlType.TextField, "input-exchangeData-text", inputIntents)
        );
        for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
            String feature = CharacterFeatures.getFeatureName(i);
            update.getControls().add(new Control(
                    Control.ControlType.TextField,
                    "input-character-"+feature+"-start",
                    lastRange != null ? lastRange.range[i].start : 0
            ));
            update.getControls().add(new Control(
                    Control.ControlType.TextField,
                    "input-character-"+feature+"-end",
                    lastRange != null ? lastRange.range[i].end : 0
            ));
        }
        pluginProxy.sendMessage(update);
    }

    public static void setOutputInForm(){
        SetPanel update = new SetPanel(
                "dialog-debug",
                SetPanel.PanelType.PANEL,
                SetPanel.ActionType.UPDATE,

                new Control(Control.ControlType.ComboBox, "input-emotion-trigger",
                        outputEmotion != null ? emotionsList.indexOf(outputEmotion) : 0),
                new Control(Control.ControlType.TextField, "output-exchangeData-text", output)
        );
        pluginProxy.sendMessage(update);
    }

    public static void updateDialogModules(){
        if (isSamePacks(Main.getCurrentCharacter().phrases))
            return;

        lastPhrasesPacksList = Main.getCurrentCharacter().phrases.toPacksList();
        List<Phrase> intentPhrases = Main.getCurrentCharacter().phrases.toPhrasesList();
        List<Phrase> characterPhrases = Main.getCurrentCharacter().phrases.toPhrasesList(PhrasesPack.PackType.USER);

        /*try {
            characterClassifier = new NeuralCharacterClassifier(Main.getCharacterClassifierModelPath());
        } catch (Exception e){
            Main.log(new Exception("Error loading model at " +  Main.getCharacterClassifierModelPath(), e));
            characterClassifier = new NeuralCharacterClassifier(phrases, Main.getCharacterClassifierModelPath());
        }*/
        characterClassifier = new NaiveCharacterClassifier(characterPhrases);
        intentClassifier = new IntentClassifier(intentPhrases);

        allIntents = new IntentList();
        for (Phrase phrase : intentPhrases)
            if (phrase.intentType != null)
                allIntents.addAll(phrase.intentType);

        if (allIntents.size() > 0) {
            allIntents.sort(Comparator.naturalOrder());
            inputIntentSelected = allIntents.get(0);
        } else {
            inputIntentSelected = "X";
        }
        outputIntentSelected = inputIntentSelected;
        resetPanel();
    }

    private static List<String> lastPhrasesPacksList = null;
    private static boolean isSamePacks(PhrasesPackList packs){
        if (lastPhrasesPacksList == null)
            return false;

        List<String> newList = packs.toPacksList();
        if (newList.size() != lastPhrasesPacksList.size())
            return false;
        for (String pack : newList){
            if (!lastPhrasesPacksList.contains(pack))
                return false;
        }
        return true;
    }

}
