package info.deskchan.talking_system.classification;

import info.deskchan.core.PluginProxyInterface;
import info.deskchan.talking_system.CharacterPreset;
import info.deskchan.talking_system.Phrase;

import java.util.HashMap;
import java.util.Map;

public class Main {

    static Classifier classifier;

    public static void initialize(PluginProxyInterface pluginProxy){

        setClassifier();

        pluginProxy.sendMessage("core:register-alternative", new HashMap<String,Object>(){{
            put("srcTag", "DeskChan:user-said");
            put("dstTag", "talk:chat-react");
            put("priority", 10);
        }});

        pluginProxy.addMessageListener("talk:chat-react", (sender, tag, data) -> {
            String text;
            if (data instanceof Map)
                text = (String) ((Map) data).getOrDefault("value", "");
            else text = data.toString();

            System.out.println(classifier.classify(text));
        });

        pluginProxy.addMessageListener("talk:character-updated", (sender, tag, data) -> {
            setClassifier();
        });
    }

    static void setClassifier(){
        CharacterPreset preset = info.deskchan.talking_system.Main.getCharacterPreset();
        classifier = new Classifier();
        for (Phrase phrase : preset.phrases.getAllPhrases())
            classifier.add(phrase);
    }
}
