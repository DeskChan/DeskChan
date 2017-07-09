package info.deskchan.core;

import org.json.JSONArray;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommandsProxy{
    public static class Link{
        public final String rule;
        public final Object msgData;
        public Link(String rule,Object msgData){
            this.rule=( rule==null || rule.equals("") ? null : rule );
            this.msgData=( msgData==null || msgData.equals("") ? null : msgData );
        }
        public boolean equals(Link link){
            boolean eq=false;
            if(rule==null){
                if(link.rule!=null) return false;
                eq=true;
            } else if(rule.equals(link.rule)) eq=true;
            if(!eq) return false;
            eq=false;
            if(msgData==null){
                if(link.msgData!=null) return false;
                eq=true;
            } else if(msgData.equals(link.msgData)) eq=true;
            return eq;
        }
    }
    private static HashMap<String,Map<String,Object>> events=new HashMap<>();
    private static HashMap<String,Map<String,Object>> commands=new HashMap<>();

    /// < event < command , rule > >
    private static HashMap < String , Map <String , ArrayList<Link> > > commandLinks=new HashMap<>();

    public static ArrayList<Link> getCommandLinks(String targetName,String commandName){
        try{
            return commandLinks.get(targetName).get(commandName);
        } catch(Exception e){
            return null;
        }
    }
    public static void addCommand(Map<String,Object> data){
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for command to add: "+data);
        else commands.put((String)data.get("tag"),data);
    }
    public static void removeCommand(String tag){
        if(tag==null)
            PluginManager.log("No name specified for command to remove, recieved null");
        else commands.remove(tag);
    }
    public static void removeCommand(Map<String,Object> data) {
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for command to remove: "+data);
        else commands.remove((String)data.get("tag"));
    }
    public static void addEvent(Map<String,Object> data){
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for event to add: "+data);
        else events.put((String)data.get("tag"),data);
    }
    public static void removeEvent(String tag) {
        if(tag==null)
            PluginManager.log("No name specified for event to remove, recieved null");
        else events.remove(tag);
    }
    public static void removeEvent(Map<String,Object> data) {
        if(!data.containsKey("tag"))
            PluginManager.log("No name specified for event to remove: "+data);
        else events.remove((String)data.get("tag"));
    }
    public static void addEventLink(String eventName,String commandName,String rule,Object msgData){
        if(eventName==null){
            PluginManager.log("No event name specified to event link");
            return;
        }
        if(commandName==null){
            PluginManager.log("No target command name specified to add event link");
            return;
        }
        Map<String,ArrayList<Link>> map=commandLinks.getOrDefault(eventName,null);
        if(map==null){
            map=new HashMap<>();
            commandLinks.put(eventName,map);
        }
        ArrayList<Link> links=map.getOrDefault(commandName,null);
        Link linktoadd=new Link(rule,msgData);
        if(links==null) {
            links = new ArrayList<>();
            map.put(commandName,links);
            links.add(linktoadd);
            return;
        }
        for(Link link : links) {
            if (link.equals(linktoadd)) return;
        }
        links.add(linktoadd);
    }
    public static void addEventLink(Map<String,Object> data){
        addEventLink((String)data.getOrDefault("eventName",null),(String)data.getOrDefault("commandName",null),
                     (String)data.getOrDefault("rule",null),data.getOrDefault("msgData",null));
    }
    public static void removeEventLink(String eventName,String commandName){
        try{
            commandLinks.get(eventName).remove(commandName);
        } catch(Exception e){
            PluginManager.log("No such event and command specified to remove event link: "+eventName+" / "+commandName);
        }
    }
    public static void removeEventLink(Map<String,Object> data){
        removeEventLink((String)data.getOrDefault("eventName",null),(String)data.getOrDefault("commandName",null));
    }
    public static Map<String,Object> getCommandsMatch(String eventName){
        HashMap<String,Object> ar=new HashMap<>();
        if(eventName==null)
            PluginManager.log("No event name specified for matching command list");
        else try{
            for(HashMap.Entry<String,ArrayList<Link>> entry : commandLinks.get(eventName).entrySet()){
                if(commands.containsKey(entry.getKey())) {
                    for (Link link : entry.getValue()) {
                        HashMap<String, Object> cpy = new HashMap<String, Object>(commands.get(entry.getKey()));
                        if (link.rule != null) cpy.put("rule", link.rule);
                        if (link.msgData != null) cpy.put("msgData", link.msgData);
                        ar.put(entry.getKey(), cpy);
                    }
                }
            }
        } catch(Exception e){
            PluginManager.log(e);
        }
        return ar;
    }
    public static ArrayList<String> getCommandsList(){
        return new ArrayList<String>(commands.keySet());
    }
    public static ArrayList<String> getEventsList(){
        return new ArrayList<String>(events.keySet());
    }
    public static ArrayList<Map<String,Object>> getLinksList(){
        ArrayList<Map<String,Object>> list=new ArrayList<>();
        for(Map.Entry<String,Map<String,ArrayList<Link>>> entry1 : commandLinks.entrySet()){
            for(Map.Entry<String,ArrayList<Link>> entry2 : entry1.getValue().entrySet()){
                for(Link link : entry2.getValue()) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("event", entry1.getKey());
                    map.put("command", entry2.getKey());
                    map.put("rule", link.rule);
                    map.put("data", link.msgData != null ? link.msgData : "");
                    list.add(map);
                }
            }
        }
        return list;
    }
    public static void resetLinks(ArrayList<Map<String,Object>> newData){
        commandLinks.clear();
        for(Map<String,Object> data : newData){
            addEventLink(data);
        }
    }
    public static void initialize(PluginProxyInterface proxy){
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
        load();
    }
    private static final File file=PluginManager.getDataDirPath().resolve("core").resolve("links").toFile();
    public static void save(){
        FileWriter writer=null;
        try {
            writer=new FileWriter(file);
        } catch(Exception e){
            PluginManager.log("Error while locate space for command links file");
            return;
        }
        BufferedWriter out=new BufferedWriter(writer);
        try {
            JSONArray array=new JSONArray();
            for(Map.Entry<String,Map<String,ArrayList<Link>>> entry1 : commandLinks.entrySet()) {
                for (Map.Entry<String, ArrayList<Link>> entry2 : entry1.getValue().entrySet()) {
                    for(Link link : entry2.getValue()) {
                        JSONArray ar = new JSONArray();
                        ar.put(entry1.getKey());
                        ar.put(entry2.getKey());
                        ar.put(link.rule != null ? link.rule : "");
                        if (link.msgData != null && link.msgData instanceof String)
                            ar.put(link.msgData);
                        array.put(ar);
                    }
                }
            }
            out.write(array.toString());
            out.flush();
            PluginManager.log("Links successfully saved");
        } catch (Exception e){
            PluginManager.log("Error while writing command links file");
        }
    }
    public static void load(){
        FileReader reader=null;
        try {
            reader=new FileReader(file);
        } catch(Exception e){
            PluginManager.log("Error while locate command links file");
            return;
        }
        BufferedReader out=new BufferedReader(reader);
        try {
            JSONArray array=new JSONArray(out.readLine());
            for(Object obj : array) {
                if(!(obj instanceof JSONArray)) continue;
                JSONArray link=(JSONArray) obj;
                if(link.length()<3 || link.getString(0).length()==0 || link.getString(1).length()==0 || link.getString(0).length()==2) continue;
                addEventLink(link.getString(0),link.getString(1),link.getString(2),link.length()>3 ? link.getString(3) : "");
            }
            PluginManager.log("Links successfully loaded");
        } catch (Exception e){
            PluginManager.log("Error while loading command links file");
            PluginManager.log(e);
        }
    }
}
