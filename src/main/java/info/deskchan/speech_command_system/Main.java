package info.deskchan.speech_command_system;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core_utils.LimitHashMap;
import info.deskchan.core_utils.TextOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main implements Plugin {
    private static PluginProxyInterface pluginProxy;
    private String start_word="пожалуйста";
    private static LimitHashMap<String, TextRule> cachedRules = new LimitHashMap<>(100);
    private static final HashMap<String,Object> standartCommandsCoreQuery=new HashMap<String,Object>(){{
        put("eventName","speech:get");
    }};

    @Override
    public boolean initialize(PluginProxyInterface newPluginProxy) {
        pluginProxy=newPluginProxy;

        log("loading speech to command module");

        pluginProxy.sendMessage("core:add-event", TextOperations.toMap("tag: \"speech:get\""));

        pluginProxy.sendMessage("core:register-alternative",new HashMap<String,Object>(){{
            put("srcTag", "DeskChan:user-said");
            put("dstTag", "speech:get");
            put("priority", 100);
        }});

        pluginProxy.addMessageListener("speech:get", (sender, tag, data) -> {
            String text=(String)((HashMap<String,Object>)data).getOrDefault("value","");
            pluginProxy.sendMessage("core:get-commands-match",standartCommandsCoreQuery,(s, d) -> {
                HashMap<String,Object> commands=(HashMap<String,Object>) d;
                operateRequest(text,(List<HashMap<String,Object>>) commands.get("commands"));
            });
        });

        log("loading speech to command module");
        return true;
    }
    static class Command{
        String tag;
        Map<String,Object> data;
        TextRule rule = null;
        public Command(Map<String,Object> map){
            tag=(String) map.get("tag");
            data = map;
            String r = (String) map.getOrDefault("rule",null);
            if(r!=null)
                rule = new TextRule(r);
        }
        public boolean better(Command other){
            return rule.result.better(other.rule.result);
        }
    }
    void operateRequest(String text, List<HashMap<String,Object>> commandsInfo){
        ArrayList<String> words = PhraseComparison.toClearWords(text);
        Command best=null;
        for(Map<String,Object> comData : commandsInfo){
            Command command =  new Command(comData);
            if(command.rule==null) {
                pluginProxy.sendMessage(command.tag, new HashMap<String, Object>() {{
                    put("text", words);
                    if (comData.containsKey("msgData"))
                        put("msgData", comData.get("msgData"));
                }});
                continue;
            }
            command.rule.match(words);
            if(command.rule.result.matchPercentage>0.5 && (best == null || command.better(best)))
                best = command;
        }
        if(best!=null) {
            HashMap<String,Object> ret = best.rule.getArguments(text, words);
            if(best.data.containsKey("msgData"))
                ret.put("msgData",best.data.get("msgData"));
            pluginProxy.sendMessage( best.tag, ret);
        }
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
