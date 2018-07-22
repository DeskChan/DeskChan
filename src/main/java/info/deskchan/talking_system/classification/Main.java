package info.deskchan.talking_system.classification;

import info.deskchan.core.PluginProxyInterface;
import info.deskchan.talking_system.CharacterPreset;
import info.deskchan.talking_system.Phrase;

import java.util.HashMap;
import java.util.Map;

public class Main {

    static Classifier classifier;
    static PluginProxyInterface pluginProxy;

    public static void initialize(PluginProxyInterface proxy){
        pluginProxy = proxy;

        setClassifier();

        pluginProxy.setAlternative("DeskChan:user-said", "talk:classify-text", 50000);

        pluginProxy.addMessageListener("talk:classify-text", (sender, tag, data) -> {
            String text;
            Map map;
            if (data instanceof Map) {
                map = (Map) data;
                text = (String) map.getOrDefault("value", "");
            } else {
                text = data.toString();
                map = new HashMap();
                map.put("value", text);
            }
            map.put("intent", classifier.classify(text));
            //System.out.println(map.get("intent"));
            pluginProxy.callNextAlternative(sender, "DeskChan:user-said", "talk:classify-text", map);
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
