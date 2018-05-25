package info.deskchan.speech_command_system;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main implements Plugin {
    private static PluginProxyInterface pluginProxy;

    /** Check this to log all rules comparison into console. **/
    protected static final boolean debugBuild = false;

    @Override
    public boolean initialize(PluginProxyInterface newPluginProxy) {
        pluginProxy = newPluginProxy;

        log("loading speech to command module");

        // Adding event information to core
        pluginProxy.sendMessage("core:add-event", new HashMap(){{
            put("tag", "speech:get");
            put("info", pluginProxy.getString("speech-get-info"));
            put("ruleInfo", pluginProxy.getString("speech-get-rule-info"));
        }});

        // Registering as alternative
        pluginProxy.sendMessage("core:register-alternative", new HashMap<String,Object>(){{
            put("srcTag", "DeskChan:user-said");
            put("dstTag", "speech:get");
            put("priority", 100);
        }});
        pluginProxy.sendMessage("core:register-alternative", new HashMap<String,Object>(){{
            put("srcTag", "DeskChan:commands-list");
            put("dstTag", "speech:commands-list");
            put("priority", 100);
        }});

        // Transforming rules to Command every time commands list updates
        pluginProxy.addMessageListener("core:update-links:speech:get", (sender, tag, data) -> {
            updateCommandsList((List) data);
        });

        /* Analyze speech and call matching command.
         * Public message
         * Params: Map
         *           value: String! - speech
         * Returns: None */
        pluginProxy.addMessageListener("speech:commands-list", (sender, tag, data) -> {
            StringBuilder sb = new StringBuilder();
            for (Command command : commands)
                sb.append(command.rule.getRule()+"\n");

            pluginProxy.sendMessage("DeskChan:say", "Я умею много всего! Сейчас список даже покажу.");
            pluginProxy.setTimer(200, (s, d) -> {
                pluginProxy.sendMessage("DeskChan:show-technical", sb.toString());
            });
        });

        /* Say commands list.
         * Public message
         * Params: None
         * Returns: None */
        pluginProxy.addMessageListener("speech:get", (sender, tag, data) -> {
            String text;
            if (data instanceof Map)
                text = (String) ((Map) data).getOrDefault("value", "");
            else text = data.toString();

            if (text == null || text.trim().length() == 0) return;
            if (!operateRequest(text))
                pluginProxy.sendMessage("DeskChan:user-said#speech:get", data);
        });

        /* Check if speech matches to rule.
         * Public message
         * Params: Map
         *           speech: String! - speech
         *           rule: String! - rule
         * Returns: Float - match value */
        pluginProxy.addMessageListener("speech:match", (sender, tag, data) -> {
            if (!(data instanceof Map)){
                throw new IllegalArgumentException();
            }

            Map query = (Map) data;
            try {
                RegularRule rule = RegularRule.create(query.get("rule").toString());
                RegularRule.MatchResult result = rule.parse(query.get("speech").toString());
                pluginProxy.sendMessage(sender, result.matchPercentage);
            } catch (Exception e){
                pluginProxy.log(e);
            }
        });

        /* Check if speech matches to any rule of list.
         * Public message
         * Params: Map
         *           speech: String! - speech
         *           rules: List<String>! - rules
         * Returns: Int - index of rule with best match or -1 if nothing matches */
        pluginProxy.addMessageListener("speech:match-any", (sender, tag, data) -> {
            if (!(data instanceof Map)){
                throw new IllegalArgumentException();
            }

            Map query = (Map) data;
            try {
                String speech = query.get("speech").toString();
                int index = -1, i=0;
                RegularRule.MatchResult bestResult = null;
                for (Object ruletext : (List) query.get("rules")){
                    RegularRule rule = RegularRule.create(ruletext.toString());
                    RegularRule.MatchResult result = rule.parse(speech);
                    if (result.better(bestResult)){
                        index = i;
                        bestResult = result;
                    }
                    i++;
                }
                pluginProxy.sendMessage(sender, index);
            } catch (Exception e){
                pluginProxy.log(e);
            }
        });

        pluginProxy.addMessageListener("recognition:get-words", (sender, tag, data) -> {
            pluginProxy.sendMessage(sender, Parsers.getWords());
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
            String r = (String) map.get("rule");
            msgData = map.get("msgData");
            RegularRule rul = null;
            if(r != null) {
                try {
                    rul = RegularRule.create(r);
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
    boolean operateRequest(String text){
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
               System.out.println(command.tag + " " + command.rule.getRule() + " " + command.result + " " + command.better(best));
            if(command.better(best))
                best = command;
        }

        if(best != null) {
            if(debugBuild)
                System.out.println("best: " + best.tag + " " + best.result);
            Map<String, Object> ret = best.rule.getArguments(text, best.result);
            if(debugBuild)
                System.out.println("1: " + ret);
            if(best.msgData != null) {
                if (best.msgData instanceof Map){
                    for(Map.Entry<String, Object> entry : ((Map<String, Object>) best.msgData).entrySet()){
                        ret.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    ret.put("msgData", best.msgData);
                }
            }
            if(debugBuild)
                System.out.println("2: " + ret);
            if (!ret.containsKey("msgData")) ret.put("msgData", text);
            if(debugBuild)
                System.out.println("3: " + ret);
            pluginProxy.sendMessage(best.tag, ret);
        }
        return best != null;
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
