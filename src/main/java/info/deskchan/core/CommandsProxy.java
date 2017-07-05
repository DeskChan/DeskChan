package info.deskchan.core;

import java.util.HashMap;

public class CommandsProxy{
    private static HashMap<String,HashMap<String,Object>> events=new HashMap<>();
    private static HashMap<String,HashMap<String,Object>> commands=new HashMap<>();

    /// < event < command , rule > >
    private static HashMap<String,HashMap<String,String>> commandLinks=new HashMap<>();

    public static String getCommandLinks(String targetName,String commandName){
        try{
            return commandLinks.get(targetName).get(commandName);
        } catch(Exception e){
            return null;
        }
    }
    public static void addCommand(HashMap<String,Object> data){
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for command to add: "+data);
        else commands.put((String)data.get("tag"),data);
    }
    public static void removeCommand(String tag){
        if(tag==null)
            PluginManager.log("No name specified for command to remove, recieved null");
        else commands.remove(tag);
    }
    public static void removeCommand(HashMap<String,Object> data) {
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for command to remove: "+data);
        else commands.remove((String)data.get("tag"));
    }
    public static void addEvent(HashMap<String,Object> data){
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for event to add: "+data);
        else events.put((String)data.get("tag"),data);
    }
    public static void removeEvent(String tag) {
        if(tag==null)
            PluginManager.log("No name specified for event to remove, recieved null");
        else events.remove(tag);
    }
    public static void removeEvent(HashMap<String,Object> data) {
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for event to remove: "+data);
        else events.remove((String)data.get("tag"));
    }
    public static void addEventLink(String eventName,String commandName,String rule){
        if(eventName==null){
            PluginManager.log("No event name specified to event link");
            return;
        }
        if(commandName==null){
            PluginManager.log("No target command name specified to add event link");
            return;
        }
        HashMap<String,String> map=commandLinks.getOrDefault(eventName,null);
        if(map==null){
            map=new HashMap<String,String>();
            commandLinks.put(eventName,map);
        }
        map.put(commandName,( rule.equals("") ? null : rule ));
    }
    public static void addEventLink(HashMap<String,Object> data){
        addEventLink((String)data.getOrDefault("eventName",null),(String)data.getOrDefault("commandName",null),(String)data.getOrDefault("rule",null));
    }
    public static void removeEventLink(String eventName,String commandName){
        try{
            commandLinks.get(eventName).remove(commandName);
        } catch(Exception e){
            PluginManager.log("No such event and command specified to remove event link: "+eventName+" / "+commandName);
        }
    }
    public static void removeEventLink(HashMap<String,Object> data){
        removeEventLink((String)data.getOrDefault("eventName",null),(String)data.getOrDefault("commandName",null));
    }
    public static HashMap<String,Object> getCommandsMatch(String eventName){
        HashMap<String,Object> ar=new HashMap<>();
        if(eventName==null)
            PluginManager.log("No event name specified for matching command list");
        else try{
            for(HashMap.Entry<String,String> entry : commandLinks.get(eventName).entrySet()){
                if(commands.containsKey(entry.getKey())) {
                    HashMap<String,Object> cpy=new HashMap<String,Object>(commands.get(entry.getKey()));
                    cpy.put("rule",entry.getValue());
                    ar.put(entry.getKey(),cpy);
                }
            }
        } catch(Exception e){
            PluginManager.log(e);
        }
        return ar;
    }

    public static void initialize(PluginProxy proxy){
        proxy.addMessageListener("core:add-command", (sender, tag, data) -> addCommand((HashMap<String,Object>) data) );
        proxy.addMessageListener("core:remove-command", (sender, tag, data) -> removeCommand((HashMap<String,Object>) data) );
        proxy.addMessageListener("core:add-event", (sender, tag, data) -> addEvent((HashMap<String,Object>) data) );
        proxy.addMessageListener("core:remove-event", (sender, tag, data) -> removeEvent((HashMap<String,Object>) data) );
        proxy.addMessageListener("core:set-event-link", (sender, tag, data) -> addEventLink((HashMap<String,Object>) data) );
        proxy.addMessageListener("core:remove-event-link", (sender, tag, data) -> removeEventLink((HashMap<String,Object>) data) );
        proxy.addMessageListener("core:get-commands-match", (sender, tag, dat) -> {
            HashMap<String,Object> data=(HashMap<String,Object>) dat;
            String eventName=(String) data.getOrDefault("eventName",null);
            Integer seq=(Integer) data.getOrDefault("seq",null);
            data=new HashMap<>();
            data.put("seq",seq);
            data.put("commands",getCommandsMatch(eventName));
            proxy.sendMessage(sender,data);
        });
    }
}
