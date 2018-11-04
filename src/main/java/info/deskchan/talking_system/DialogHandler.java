package info.deskchan.talking_system;

import info.deskchan.core.MessageDataMap;
import info.deskchan.core.MessageListener;
import info.deskchan.core.PluginProxyInterface;

import java.util.*;

public class DialogHandler {
    static IntentExchanger intentExchanger;
    static CharacterClassifier characterClassifier;
    static IntentClassifier intentClassifier;

    static PluginProxyInterface pluginProxy;

    private static IntentList allIntents;

    private static CharacterRange lastRange;
    private static IntentList lastIntents;

    private static String phraseText = "";
    private static String inputIntents = "";
    private static String inputIntentSelected = "";
    private static String outputIntents = "";
    private static String outputIntentSelected = "";
    private static String outputEmotion = null;
    private static List<String> emotionsList = null;
    private static IntentExchanger.DialogLine lastRequest = null;
    private static IntentExchanger.DialogLine lastAnswer = null;

    public static void initialize(){

        pluginProxy = Main.getPluginProxy();

        updateDialogModules();
        intentExchanger = new IntentExchanger(Main.getCurrentCharacter().character);
        intentExchanger.next();

        pluginProxy.setAlternative("DeskChan:user-said", "talk:classify-text", 50000);
        pluginProxy.setAlternative("DeskChan:user-said", "talk:dialog-receive", 25);

        pluginProxy.addMessageListener("talk:classify-text", (sender, tag, data) -> {
            MessageDataMap map = new MessageDataMap("value", data);
            List<String> intents = intentClassifier.classify(map.getString("value"));
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

            phraseText = map.getString("value");
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

            CharacterPreset currentCharacter = Main.getCurrentCharacter();

            lastRequest = new IntentExchanger.DialogLine(
                    new IntentExchanger.Reaction(lastIntents, currentCharacter.emotionState.getCurrentEmotionName()),
                    lastRange.toCentersArray(),
                    false
            );
            IntentExchanger.DialogLine answer = intentExchanger.next(lastRequest);

            outputIntents = answer.reaction.intents.toString();

            outputEmotion = answer.reaction.emotion;
            if (answer.reaction.emotion != null)
                currentCharacter.emotionState.raiseEmotion(answer.reaction.emotion);


            for (String intent : answer.reaction.intents)
                Main.phraseRequest(intent);

            lastAnswer = answer;

            setInputInForm();
            setOutputInForm();
            Main.getInstance().resetTimer();
        });

        pluginProxy.addMessageListener("talk-debug:classify-text", (sender, tag, data) -> {
            MessageDataMap map = new MessageDataMap("value", data);
            map.put("intent", intentClassifier.classify(map.getString("value")));
            map.put("character", characterClassifier.getCharacterForPhrase(map.getString("value")).toMap());

            setInputInForm();

            pluginProxy.callNextAlternative(sender, "DeskChan:user-said", "talk:classify-text", map);
        });

        pluginProxy.addMessageListener("talk-debug:phrase-text-changed", (sender, tag, data) -> {
            phraseText = (String) data;
        });


        for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
            String feature = CharacterRange.getFeatureName(i);
            pluginProxy.addMessageListener("talk-debug:set-input-character-"+feature+"-start", inputCharacterChanged);
            pluginProxy.addMessageListener("talk-debug:set-input-character-"+feature+"-end",   inputCharacterChanged);
        }

        pluginProxy.addMessageListener("talk-debug:input-intents-changed", (sender, tag, data) -> {
            inputIntents = (String) data;
        });

        pluginProxy.addMessageListener("talk-debug:input-intents-selected", (sender, tag, data) -> {
            inputIntentSelected = (String) data;
        });

        pluginProxy.addMessageListener("talk-debug:input-intents-clicked", (sender, tag, data) -> {
            IntentList in = new IntentList(inputIntents);
            in.add(inputIntentSelected);
            inputIntents = in.toString();
            setInputInForm();
        });

        pluginProxy.addMessageListener("talk-debug:save-input", (sender, tag, data) -> {
            PhrasesPack pack = Main.getCurrentCharacter().phrases.getUserDatabasePack();
            Phrase newPhrase = new Phrase(phraseText);
            newPhrase.character = lastRange;
            newPhrase.setIntents(new IntentList(inputIntents));
            lastRequest.reaction.intents = new IntentList(newPhrase.intentType);
            pack.add(newPhrase);
            pack.save();
            intentClassifier.add(newPhrase);
            characterClassifier.add(newPhrase);
        });

        pluginProxy.addMessageListener("talk-debug:output-intents-changed", (sender, tag, data) -> {
            outputIntents = (String) data;
        });

        pluginProxy.addMessageListener("talk-debug:output-intents-selected", (sender, tag, data) -> {
            outputIntentSelected = (String) data;
        });

        pluginProxy.addMessageListener("talk-debug:output-intents-clicked", (sender, tag, data) -> {
            IntentList in = new IntentList(outputIntents);
            in.add(outputIntentSelected);
            outputIntents = in.toString();
            setOutputInForm();
        });

        pluginProxy.addMessageListener("talk-debug:output-emotion-changed", (sender, tag, data) -> {
            outputEmotion = (String) data;
        });

        pluginProxy.addMessageListener("talk-debug:show-history", (sender, tag, data) -> {
            pluginProxy.sendMessage("DeskChan:show-technical", new HashMap<String, Object>(){{
                put("name", Main.getString("dialog-debug.history"));
                put("text", intentExchanger.getHistory());
            }});
        });


        pluginProxy.addMessageListener("talk-debug:save-output", (sender, tag, data) -> {
            IntentList output = new IntentList(outputIntents);
            intentExchanger.deleteUntil(lastAnswer,
                new IntentExchanger.Reaction(
                        output, outputEmotion
                )
            );

            Main.phraseRequest("CORRECTED");

            for (String intent : output)
                Main.phraseRequest(intent);
        });

        pluginProxy.addMessageListener("talk-debug:save-dialog-log", (sender, tag, data) -> {
            intentExchanger.saveCurrentDialog();
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
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("type", "panel");
            put("action", "set");
            put("id", "dialog-debug");
            put("name", Main.getString("dialog-debug"));

            List<Map<String, Object>> list = new LinkedList<>();
            put("controls", list);

            list.add(new HashMap<String, Object>() {{
                put("type", "Button");
                put("value", Main.getString("dialog-debug.show-history"));
                put("msgTag", "talk-debug:show-history");
            }});

            list.add(new HashMap<String, Object>() {{
                put("type", "TextField");
                put("value", "");
                put("id", "phraseText");
                put("onChangeTag", "talk-debug:phrase-text-changed");
            }});

            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", Main.getString("dialog-debug.phrase-character-diapason"));
            }});
            for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
                String feature = CharacterFeatures.getFeatureName(i);
                List<Map> row = new ArrayList<>();
                Map<String, Object> ch = new HashMap<>();
                ch.put("id", "input-character-"+feature+"-start");
                ch.put("value", 0);
                ch.put("type", "Spinner");
                ch.put("min", -CharacterFeatures.BORDER);
                ch.put("max",  CharacterFeatures.BORDER);
                ch.put("step", 1);
                ch.put("msgTag", "talk-debug:set-input-character-"+feature+"-start");
                row.add(ch);
                ch = new HashMap<>(ch);
                ch.put("id", "input-character-"+feature+"-end");
                ch.put("msgTag", "talk-debug:set-input-character-"+feature+"-end");
                row.add(ch);
                ch = new HashMap<>();
                ch.put("elements", row);
                ch.put("label", Main.getString(CharacterFeatures.getFeatureName(i)));
                ch.put("hint", Main.getString("help."+ CharacterFeatures.getFeatureName(i)));
                list.add(ch);
            }


            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", Main.getString("dialog-debug.phrase-intents"));
            }});
            Map intentsControl = new HashMap<String, Object>();
            intentsControl.put("elements", new ArrayList<Map>(){{
                add(new HashMap<String, Object>() {{
                    put("id", "input-intents-text");
                    put("type", "TextField");
                    put("onChangeTag", "talk-debug:input-intents-changed");
                }});
                add(new HashMap<String, Object>() {{
                    put("type", "ComboBox");
                    put("values", allIntents);
                    put("msgTag", "talk-debug:input-intents-selected");
                }});
                add(new HashMap<String, Object>() {{
                    put("type", "Button");
                    put("value", Main.getString("add"));
                    put("msgTag", "talk-debug:input-intents-clicked");
                }});
            }});
            list.add(intentsControl);

            list.add(new HashMap<String, Object>() {{
                put("type", "Button");
                put("value", Main.getString("save"));
                put("msgTag", "talk-debug:save-input");
            }});


            list.add(new HashMap<String, Object>() {{
                put("type", "Separator");
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", Main.getString("dialog-debug.answer-intents"));
            }});
            intentsControl = new HashMap<String, Object>();
            intentsControl.put("elements", new ArrayList<Map>(){{
                add(new HashMap<String, Object>() {{
                    put("id", "output-intents-text");
                    put("type", "TextField");
                    put("onChangeTag", "talk-debug:output-intents-changed");
                }});
                add(new HashMap<String, Object>() {{
                    put("type", "ComboBox");
                    put("values", allIntents);
                    put("msgTag", "talk-debug:output-intents-selected");
                }});
                add(new HashMap<String, Object>() {{
                    put("type", "Button");
                    put("value", Main.getString("add"));
                    put("msgTag", "talk-debug:output-intents-clicked");
                }});
            }});
            list.add(intentsControl);


            list.add(new HashMap<String, Object>() {{
                put("type", "Separator");
            }});

            emotionsList = Main.getCurrentCharacter().emotionState.getEmotionsList(); emotionsList.add(0, "");
            list.add(new HashMap<String, Object>() {{
                put("id", "emotion-trigger");
                put("type", "ComboBox");
                put("label", Main.getString("dialog-debug.emotion-triggers"));
                put("values", emotionsList);
                put("msgTag", "talk-debug:output-emotion-changed");
            }});

            list.add(new HashMap<String, Object>() {{
                put("type", "Button");
                put("value", Main.getString("save"));
                put("msgTag", "talk-debug:save-output");
            }});

            list.add(new HashMap<String, Object>() {{
                put("type", "Button");
                put("value", Main.getString("dialog-debug.save-dialog"));
                put("msgTag", "talk-debug:save-dialog-log");
            }});

        }});
    }

    public static void setInputInForm(){
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("action", "update");
            put("id", "dialog-debug");

            List<Map<String, Object>> list = new LinkedList<>();
            put("controls", list);

            list.add(new HashMap<String, Object>() {{
                put("value", phraseText);
                put("id", "phraseText");
            }});
            for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
                String feature = CharacterFeatures.getFeatureName(i);
                Map<String, Object> ch = new HashMap<>();
                ch.put("id", "input-character-"+feature+"-start");
                ch.put("value", lastRange.range[i].start);
                list.add(ch);
                ch = new HashMap<>(ch);
                ch.put("id", "input-character-"+feature+"-end");
                ch.put("value", lastRange.range[i].end);
                list.add(ch);
            }

            list.add(new HashMap<String, Object>() {{
                put("id", "input-intents-text");
                put("value", inputIntents);
            }});
        }});
    }

    public static void setOutputInForm(){
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("action", "update");
            put("id", "dialog-debug");

            List<Map<String, Object>> list = new LinkedList<>();
            put("controls", list);


            list.add(new HashMap<String, Object>() {{
                put("id", "output-intents-text");
                put("value", outputIntents);
            }});

            emotionsList = Main.getCurrentCharacter().emotionState.getEmotionsList(); emotionsList.add(0, "");
            list.add(new HashMap<String, Object>() {{
                put("id", "emotion-trigger");
                put("value", outputEmotion != null ? emotionsList.indexOf(outputEmotion) : 0);
            }});
        }});
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
