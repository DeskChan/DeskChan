package info.deskchan.speech_command_system;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core_utils.TextOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main implements Plugin {
    private static PluginProxyInterface pluginProxy;

    /** Check this to log all rules comparison into console. **/
    private static final boolean debugBuild = false;

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

        pluginProxy.addMessageListener("core:update-links#speech:get", (sender, tag, data) -> {
            updateCommandsList((List) data);
        });

        pluginProxy.addMessageListener("speech:get", (sender, tag, data) -> {
            String text;
            if (data instanceof Map)
                 text = (String) ((Map) data).getOrDefault("value", "");
            else text = data.toString();

            operateRequest(text);
        });

        log("loading completed");
        return true;
    }

    static class Command {
        final String tag;
        final RegularRule rule;
        final Object msgData;
        RegularRule.MatchResult result;

        public Command(Map<String, Object> map){
            tag = (String) map.get("tag");
            String r = (String) map.getOrDefault("rule", null);
            msgData = map.get("msgData");
            RegularRule rul = null;
            if(r != null) {
                try {
                    rul = new RegularRule(r);
                } catch (Exception e) {
                    Main.log(e);
                }
            }
            rule = rul;
        }

        public boolean better(Command other){
            return result.better(other != null ? other.result : null);
        }
    }

    static Command[] commands;

    /** Preparing command list for comparison. **/
    void updateCommandsList(List<Map<String,Object>> commandsInfo){
        try {
            Command[] newCommands = new Command[commandsInfo.size()];
            for (int i = 0; i < commandsInfo.size(); i++)
                newCommands[i] = new Command(commandsInfo.get(i));

            commands = newCommands;
        } catch (Exception e){
            Main.log("Error while updating links list");
        }
    }

    /** Operate user speech request with commands. **/
    void operateRequest(String text){
        ArrayList<String> words = PhraseComparison.toClearWords(text);
        Command best = null;
        for(Command command : commands){
            if(command.rule == null) {
                pluginProxy.sendMessage(command.tag, new HashMap<String, Object>() {{
                    put("text", words);
                    if (command.msgData != null)
                        put("msgData", command.msgData);
                }});
                continue;
            }

            command.result = command.rule.parse(text, words);
            if(debugBuild)
               System.out.println(command.tag+" "+command.result+" "+command.better(best));
            if(command.better(best))
                best = command;
        }

        if(best != null) {
            if(debugBuild)
                System.out.println("best: "+best.tag+" "+best.result);
            HashMap<String,Object> ret = best.rule.getArguments();
            ret.put("text", words);
            if(best.msgData != null)
                ret.put("msgData", best.msgData);

            pluginProxy.sendMessage(best.tag, ret);
        }
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
