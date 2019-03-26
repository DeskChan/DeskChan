package info.deskchan.talking_system;

import info.deskchan.MessageData.DeskChan.ShowTechnical;
import info.deskchan.MessageData.GUI.Control;
import info.deskchan.MessageData.GUI.InlineControls;
import info.deskchan.MessageData.GUI.SetPanel;
import info.deskchan.core.MessageDataMap;
import info.deskchan.core.MessageListener;
import info.deskchan.core.Path;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core_utils.TextOperations;
import info.deskchan.talking_system.speech_exchange.*;

import java.util.*;

public class DialogHandler {

    static SpeechExchanger speechExchanger;
    static CharacterClassifier characterClassifier;
    static IntentClassifier intentClassifier;

    static PluginProxyInterface pluginProxy;

    private static IntentList allIntents;
    private static List<String> emotionsList = null;

    private static String inputPhraseText = "";
    private static String inputIntentsText = "";
    private static String inputEmotionText = null;
    private static String inputIntentSelected = "";

    private static String outputText = "";
    private static String outputEmotionText = null;
    private static String outputIntentSelected = "";
    private static boolean outputIntentsType = true;

    private static CharacterRange lastRange;
    private static DialogLine lastRequest = null;
    private static DialogLine lastResponse = null;
    
    private static List<String> bannedIntents = Arrays.asList("SET_TOPIC", "WHAT_YOUR_OPINION", "QUESTION");

    public static void initialize(){

        pluginProxy = Main.getPluginProxy();

        updateDialogModules();
        speechExchanger = new SpeechExchanger(Main.getCurrentCharacter().character);
        speechExchanger.loadLogs(Main.getDialogLogsDirPath());
        try {
            speechExchanger.loadLog(new Path(DialogHandler.class.getResource("TestDialog.log").toURI()));
        } catch (Exception e){
            Main.log(e);
        }
        Main.log("Intent exchanger loading completed");

        pluginProxy.setAlternative("DeskChan:user-said", "talk:classify-text", 50000);
        pluginProxy.setAlternative("DeskChan:user-said", "talk:dialog-receive", 25);

        pluginProxy.addMessageListener("talk:classify-text", (sender, tag, data) -> {
            MessageDataMap map = new MessageDataMap("value", data);
            IntentList intents = intentClassifier.classify(map.getString("value"));
            lastRange = characterClassifier.getCharacterForPhrase(map.getString("value"));
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
            IntentsData inputIntents;
            if (!map.containsKey("intent")) {
                inputIntents = new IntentsData(intentClassifier.classify(map.getString("value")));
            } else {
                inputIntents = new IntentsData((List<String>) map.get("intent"));
            }
            inputIntentsText = inputIntents.toString();
            IExchangeable input = inputIntents;
            if (inputIntents.size() == 0) {
                input = new PhraseData(inputPhraseText);
            }

            if (!map.containsKey("character")) {
                lastRange = characterClassifier.getCharacterForPhrase(map.getString("value"));
            } else {
                lastRange = new CharacterRange((Map) map.get("character"));
            }
            inputEmotionText = null;

            lastRequest = new DialogLine(
                    new Reaction(input, inputEmotionText),
                    lastRange.toCentersArray(),
                    DialogLine.Author.USER
            );

            DialogLine answer = speechExchanger.next(lastRequest);
            outputText = answer.getReaction().getExchangeData().toString();
            outputEmotionText = answer.getReaction().getEmotion();
            if (outputEmotionText != null)
                Main.getCurrentCharacter().emotionState.raiseEmotion(outputEmotionText);


            if (answer.getReaction().getExchangeData() instanceof PhraseData){

                PhraseData phrase = (PhraseData) answer.getReaction().getExchangeData();
                Main.sendPhrase(new Phrase(phrase.toString()), null);

            } else if (answer.getReaction().getExchangeData() instanceof IntentsData){

                IntentList list = (IntentList) answer.getReaction().getExchangeData();
                list.removeAll(bannedIntents);
                if (list.size() == 0){
                    list.add("NOTHING");
                }
                for (String intent : list)
                    Main.phraseRequest(intent);

            } else {
                throw new RuntimeException("There is a bug in dialog handler.");
            }

            lastResponse = answer;

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
            inputIntentsText = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:input-exchangeData-selected", (sender, tag, data) -> {
            inputIntentSelected = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:input-exchangeData-clicked", (sender, tag, data) -> {
            IntentList in = new IntentList(inputIntentsText);
            in.add(inputIntentSelected);
            inputIntentsText = in.toString();
            setInputInForm();
        });

        pluginProxy.addMessageListener("dialog-debug:save-input", (sender, tag, data) -> {
            IntentsData intents = new IntentsData(inputIntentsText);

            IExchangeable input = intents;

            Phrase newPhrase = new Phrase(inputPhraseText);
            newPhrase.character = lastRange;

            if (intents.size() == 0) {
                input = new PhraseData(inputPhraseText);
            } else {
                newPhrase.setIntents(intents);

                PhrasesPack pack = Main.getCurrentCharacter().phrases.getUserDatabasePack();
                pack.add(newPhrase);
                pack.save();

                intentClassifier.add(newPhrase);
            }
            characterClassifier.add(newPhrase);

            lastRequest.setReaction(new Reaction(
                    input,
                    inputEmotionText
            ));
            lastRequest.setCharacter(lastRange.toCentersArray());
        });

        pluginProxy.addMessageListener("dialog-debug:output-exchangeData-changed", (sender, tag, data) -> {
            outputText = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:output-exchangeData-selected", (sender, tag, data) -> {
            outputIntentSelected = (String) data;
        });

        pluginProxy.addMessageListener("dialog-debug:output-exchangeData-clicked", (sender, tag, data) -> {
            IntentList in = new IntentList(outputText);
            in.add(outputIntentSelected);
            outputText = in.toString();
            setOutputInForm();
        });

        pluginProxy.addMessageListener("dialog-debug:input-emotion-changed", (sender, tag, data) -> {
            inputEmotionText = (String) data;
            if (inputEmotionText.length() == 0) inputEmotionText = null;
        });

        pluginProxy.addMessageListener("dialog-debug:output-emotion-changed", (sender, tag, data) -> {
            outputEmotionText = (String) data;
            if (outputEmotionText.length() == 0) outputEmotionText = null;
        });

        pluginProxy.addMessageListener("dialog-debug:apply-input-emotion", (sender, tag, data) -> {
            lastRequest.setReaction(new Reaction(lastRequest.getReaction().getExchangeData(), inputEmotionText));
        });

        pluginProxy.addMessageListener("dialog-debug:apply-output-emotion", (sender, tag, data) -> {
            lastResponse.setReaction(new Reaction(lastResponse.getReaction().getExchangeData(), outputEmotionText));
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
            outputIntentsType = data.equals("intents");
            pluginProxy.sendMessage(new SetPanel(
                    "dialog-debug",
                    SetPanel.PanelType.PANEL,
                    SetPanel.ActionType.UPDATE,
                    new Control(
                            Control.ControlType.ComboBox,
                            "output-intent-list", null,
                            "disabled", !outputIntentsType
                    ),
                    new Control(
                            Control.ControlType.Button,
                            "output-intent-add-button", null,
                            "disabled", !outputIntentsType
                    )
            ));
        });


        pluginProxy.addMessageListener("dialog-debug:save-output", (sender, tag, data) -> {

            IntentsData intents = new IntentsData(outputText);

            IExchangeable output = intents;

            if (!outputIntentsType || intents.size() == 0) {
                output = new PhraseData(outputText);
            }

            speechExchanger.deleteUntil(lastResponse, new Reaction(output, outputEmotionText));

            Main.phraseRequest("CORRECTED");

            if (outputIntentsType && intents.size() > 0) {
                for (String intent : intents)
                    Main.phraseRequest(intent);
            } else {
                Main.sendPhrase(new Phrase(outputText), null);
            }
        });

        pluginProxy.addMessageListener("dialog-debug:save-dialog-log", (sender, tag, data) -> {
            if (!Main.getDialogLogsDirPath().exists())
                Main.getDialogLogsDirPath().mkdir();

            speechExchanger.saveCurrentDialog(Main.getDialogLogsDirPath().resolve("log-" + new Date().getTime() + ".txt"));
        });

        pluginProxy.addMessageListener("dialog-debug:reset-dialog", (sender, tag, data) -> {
            speechExchanger.setNewDialog();
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
        emotionsList = Main.getCurrentCharacter().emotionState.getEmotionsList();
        List<String> emotionsNamesList = new LinkedList<>();
        for (String value : emotionsList)
            emotionsNamesList.add(Main.getString("emotion." + value));
        emotionsList.add(0, "");
        emotionsNamesList.add(0, "");

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
                                "valuesNames", emotionsNamesList,
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
                                "valuesNames", emotionsNamesList,
                                "msgTag", "dialog-debug:output-emotion-changed"
                        ),
                        new Control(
                                Control.ControlType.Button, null,
                                Main.getString("choose"),
                                "msgTag", "dialog-debug:apply-output-emotion"
                        )
                ),
                new Control(Control.ControlType.Separator),
                new InlineControls(
                        new Control(
                                Control.ControlType.Button, null,
                                Main.getString("dialog-debug.save-dialog"),
                                "msgTag", "dialog-debug:save-dialog-log"
                        ),
                        new Control(
                                Control.ControlType.Button, null,
                                Main.getString("dialog-debug.reset-dialog"),
                                "msgTag", "dialog-debug:reset-dialog"
                        )
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
                new Control(Control.ControlType.TextField, "input-exchangeData-text", inputIntentsText)
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
                        outputEmotionText != null ? emotionsList.indexOf(outputEmotionText) : 0),
                new Control(Control.ControlType.TextField, "output-exchangeData-text", outputText)
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
