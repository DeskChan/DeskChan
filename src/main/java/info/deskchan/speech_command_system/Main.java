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
    private String start_word="пожалуйста";

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
            ArrayList<String> words = PhraseComparison.toClearWords(text);
            //if(PhraseComparison.Similar(words.get(0),start_word)<0.7) return;
            //words.remove(0);
            pluginProxy.sendMessage("core:get-commands-match",standartCommandsCoreQuery,(s, d) -> {
                HashMap<String,Object> commands=(HashMap<String,Object>) d;
                operateRequest(words,(List<HashMap<String,Object>>) commands.get("commands"));
            });
        });

        log("loading speech to command module");
        return true;
    }

    void operateRequest(ArrayList<String> words, List<HashMap<String,Object>> commandsInfo){
        float max_result=0;
        int max_words_used_count=0;
        Map<String,Object> match_command_data=null;
        String match_command_name=null;
        ArrayList<Argument> match_arguments=null;
        boolean[] max_used=null;
        for(Map<String,Object> command : commandsInfo){
            String tag=(String) command.get("tag");
            String rule=(String) command.getOrDefault("rule",null);
            if(rule==null) {
                pluginProxy.sendMessage(tag, new HashMap<String, Object>() {{
                    put("text", words);
                    if (command.containsKey("msgData"))
                        put("msgData", command.get("msgData"));
                }});
                continue;
            }
            ArrayList<String> rule_words = PhraseComparison.toRuleWords(rule);
            ArrayList<Argument> arguments=new ArrayList<>();
            for(int i=0;i<rule_words.size();i++){
                Argument arg=Argument.create(rule_words.get(i));
                if(arg==null) continue;
                if(i>0){
                    if(rule_words.get(i-1).charAt(0)=='!')
                        for(Argument argument : arguments){
                            if(argument.name.equals(rule_words.get(i-1).substring(1))){
                                arg.lastWord=argument;
                                break;
                            }
                        }
                    else arg.lastWord=rule_words.get(i-1);
                }
                arguments.add(arg);
                rule_words.set(i,"!"+arg.name);
            }
            boolean[] used=new boolean[words.size()];
            for(int i=0;i<words.size();i++) used[i]=false;
            float result=0;
            int count=0;
            int usedCount=0;
            for(int k=0;k<rule_words.size();k++){
                if(rule_words.get(k).charAt(0)=='!') continue;
                count++;
                float cur_res=0;
                int cur_pos=-1;
                for(int i=0;i<words.size();i++){
                    if(used[i]) continue;
                    float r=PhraseComparison.Levenshtein(words.get(i),rule_words.get(k));
                    float r2=1-r/(Math.max(words.get(i).length(),rule_words.get(k).length()));
                    if(r<2 && r2>0.7 && r2>cur_res){
                        cur_res=r2;
                        cur_pos=i;
                    }
                }
                if(cur_pos<0) continue;
                usedCount++;
                result+=cur_res;
                used[cur_pos]=true;
            }
            result/=count;
            if((result>max_result && result>0.5) || usedCount>max_words_used_count){
                max_result=result;
                match_command_name=tag;
                match_command_data=command;
                max_used=used;
                match_arguments=arguments;
                max_words_used_count=usedCount;
            }
        }
        if(match_command_name!=null) {
            for(Argument arg : match_arguments)
                arg.localize(words,max_used);
            HashMap<String,Object> ret=new HashMap<>();
            for(Argument arg : match_arguments)
                ret.put(arg.name, arg.value);
            for (int i = max_used.length - 1; i >= 0; i--) {
                if(max_used[i]) words.remove(i);
            }
            if(match_command_data.containsKey("msgData"))
                ret.put("msgData",match_command_data.get("msgData"));
            pluginProxy.sendMessage( match_command_name, ret);
        }
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
