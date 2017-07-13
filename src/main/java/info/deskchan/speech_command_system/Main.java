package info.deskchan.speech_command_system;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;

import java.util.ArrayList;
import java.util.HashMap;
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
        pluginProxy.sendMessage("core:add-event",new HashMap<String,Object>(){{
            put("tag","speech:get");
        }});
        pluginProxy.addMessageListener("chat:user-said", (sender, tag, data) -> {
            String text=(String)((HashMap<String,Object>)data).getOrDefault("value","");
            ArrayList<String> words = TextOperations.toClearWords(text);
            if(TextOperations.Similar(words.get(0),start_word)<0.7) return;
            words.remove(0);
            pluginProxy.sendMessage("core:get-commands-match",standartCommandsCoreQuery,(s, d) -> {
                HashMap<String,Object> commands=(HashMap<String,Object>) d;
                commands=(HashMap<String,Object>)commands.get("commands");
                operateRequest(words,commands);
            });
        });

        pluginProxy.sendMessage("core:add-command",new HashMap<String,Object>(){{
            put("tag","speech:test-command");
        }});
        pluginProxy.addMessageListener("speech:test-command", (sender, tag, data) -> {
            pluginProxy.sendMessage("DeskChan:say",new HashMap<String,Object>(){{
                put("text","Ты звал меня, душечка?");
            }});
        });
        pluginProxy.sendMessage("core:set-event-link",new HashMap<String,Object>(){{
            put("eventName","speech:get");
            put("commandName","speech:test-command");
            put("rule","скажи что нибудь");
        }});
        log("loading speech to command module");
        return true;
    }

    void operateRequest(ArrayList<String> words,HashMap<String,Object> commandsInfo){
        float max_result=0;
        String match_command_tag=null;
        boolean[] max_used=null;
        for(Map.Entry<String,Object> commandEntry : commandsInfo.entrySet()){
            HashMap<String,Object> command=(HashMap<String,Object>) commandEntry.getValue();
            String rule=(String) command.getOrDefault("rule",null);
            if(rule==null){
                pluginProxy.sendMessage((String) command.get("tag"),new HashMap<String,Object>(){{
                    put("text",words);
                }});
                continue;
            }
            ArrayList<String> rule_words = TextOperations.toClearWords(rule);
            boolean[] used=new boolean[words.size()];
            for(int i=0;i<words.size();i++) used[i]=false;
            float result=0;
            for(int k=0;k<rule_words.size();k++){
                float cur_res=0;
                int cur_pos=-1;
                for(int i=0;i<words.size();i++){
                    if(used[i]) continue;
                    float r=TextOperations.Similar(words.get(i),rule_words.get(k));
                    if(r>0.5 && r>cur_res){
                        cur_res=r;
                        cur_pos=i;
                    }
                }
                if(cur_pos<0) continue;
                result+=cur_res;
                used[cur_pos]=true;
            }
            result/=words.size();
            if(result>max_result){
                result=max_result;
                match_command_tag=(String)command.get("tag");
                max_used=used;
            }
        }
        if(match_command_tag!=null) {
            HashMap<String,Object> ret=new HashMap<>();
            for (int i = max_used.length - 1; i >= 0; i--) {
                if(max_used[i]) words.remove(i);
            }
            if(words.size()>0)
                ret.put("text",words);
            pluginProxy.sendMessage(match_command_tag, ret);
        }
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
