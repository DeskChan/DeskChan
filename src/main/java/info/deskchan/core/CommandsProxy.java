package info.deskchan.core;

import org.json.JSONArray;

import java.io.*;
import java.util.*;

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

    private static class LinkContainer{
        // < event < command , link > >
        private HashMap < String , Map <String , ArrayList<Link> > > array=new HashMap<>();
        public ArrayList<Link> getLinks(String targetName, String commandName){
            try{
                return array.get(targetName).get(commandName);
            } catch(Exception e){
                return null;
            }
        }
        public void add(String eventName, String commandName, Link link){
            Map<String,ArrayList<Link>> map=array.getOrDefault(eventName,null);
            if(map==null)
                array.put(eventName,map=new HashMap<>());

            ArrayList<Link> links=map.getOrDefault(commandName,null);
            if(links==null) {
                map.put(commandName,links = new ArrayList<>());
                links.add(link);
                return;
            }
            for(Link at : links)
                if (at.equals(link)) return;
            links.add(link);
        }
        public void remove(String eventName,String commandName) throws Exception{
            array.get(eventName).remove(commandName);
        }
        public Map<String,ArrayList<Link>> get(String eventName){
            return array.get(eventName);
        }
        public Set<Map.Entry<String,Map<String,ArrayList<Link>>>> entrySet(){
            return array.entrySet();
        }
        public void clear(){
            array.clear();
        }
        public JSONArray toJSONArray(){
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
            return array;
        }
        public ArrayList<Map<String,Object>> getLinksList(){
            ArrayList<Map<String,Object>> list=new ArrayList<>();
            for(Map.Entry<String,Map<String,ArrayList<Link>>> entry1 : array.entrySet()){
                for(Map.Entry<String,ArrayList<Link>> entry2 : entry1.getValue().entrySet()){
                    for(Link link : entry2.getValue()) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("event", entry1.getKey());
                        map.put("command", entry2.getKey());
                        map.put("rule", link.rule);
                        map.put("msgData", link.msgData != null ? link.msgData : "");
                        list.add(map);
                    }
                }
            }
            return list;
        }
        public LinkContainer clone(){
            LinkContainer lc=new LinkContainer();
            for(Map.Entry<String,Map<String,ArrayList<Link>>> entry1 : array.entrySet()) {
                Map<String,ArrayList<Link>> map=new HashMap<>();
                lc.array.put(entry1.getKey(), map);
                for (Map.Entry<String, ArrayList<Link>> entry2 : entry1.getValue().entrySet()) {
                    ArrayList<Link> list=new ArrayList<>();
                    map.put(entry2.getKey(), list);
                    for (Link link : entry2.getValue()) {
                        list.add(new Link(link.rule,link.msgData));
                    }
                }
            }
            return lc;
        }
    }

    private static LinkContainer commandLinks=new LinkContainer();
    private static LinkContainer defaultCommandLinks=new LinkContainer();

    public static void reset(){
        commandLinks=defaultCommandLinks.clone();
    }
    public static void addCommand(String sender, Map<String,Object> data){
        String tag=(String)data.getOrDefault("tag", null);
        if(tag==null)
            PluginManager.log("No name specified for command to add: "+data);
        else {
            data.remove("tag",tag);
            data.put("owner",sender);
            commands.put(tag,data);
        }
    }
    public static void removeCommand(String sender, String tag){
        if (tag == null) {
            PluginManager.log("No name specified for command to remove, recieved null");
            return;
        }
        Map<String,Object> command = commands.get(tag);
        if(sender.equals("core") || command.get("owner").equals(sender))
            commands.remove(tag);
        else
            PluginManager.log("Plugin \""+sender+"\" is not owner of command \""+tag+"\"");
    }
    public static void addEvent(String sender, Map<String,Object> data){
        String tag=(String)data.getOrDefault("tag", null);
        if(tag==null)
            PluginManager.log("No name specified for event to add: "+data);
        else {
            data.remove("tag",tag);
            data.put("owner",sender);
            events.put(tag,data);
        }
    }
    public static void removeEvent(String sender, String tag){
        if (tag == null) {
            PluginManager.log("No name specified for command to remove, recieved null");
            return;
        }
        Map<String,Object> event = events.get(tag);
        if(sender.equals("core") || event.get("owner").equals(sender))
            events.remove(tag);
        else
            PluginManager.log("Plugin \""+sender+"\" is not owner of command \""+tag+"\"");
    }
    public static void addEventLink(String eventName, String commandName, String rule, Object msgData, boolean isDefault){
        if(eventName==null){
            PluginManager.log("No event name specified to event link");
        }
        if(commandName==null){
            PluginManager.log("No target command name specified to add event link");
        }
        Link link=new Link(rule,msgData);
        if(isDefault)
            defaultCommandLinks.add(eventName, commandName, link);
         commandLinks.add(eventName, commandName, link);

    }
    public static void addEventLink(Map<String,Object> data){
        addEventLink((String)data.getOrDefault("eventName",null),(String)data.getOrDefault("commandName",null),
                     (String)data.getOrDefault("rule",null),data.getOrDefault("msgData",null),"true".equals(data.getOrDefault("isDefault","true")));
    }
    public static void removeEventLink(String eventName,String commandName){
        try {
            commandLinks.remove(eventName,commandName);
        } catch(Exception e){
            PluginManager.log("No such event and command specified to remove event link: "+eventName+" / "+commandName);
        }
        try {
            defaultCommandLinks.remove(eventName,commandName);
        } catch(Exception e){ }
    }
    public static void removeEventLink(Map<String,Object> data){
        removeEventLink((String)data.getOrDefault("eventName",null),(String)data.getOrDefault("commandName",null));
    }
    public static ArrayList<HashMap<String,Object>> getCommandsMatch(String eventName){
        ArrayList<HashMap<String,Object>> ar=new ArrayList<>();
        if(eventName==null)
            PluginManager.log("No event name specified for matching command list");
        else try {
            if(commandLinks.get(eventName)==null) return ar;
            for(HashMap.Entry<String,ArrayList<Link>> entry : commandLinks.get(eventName).entrySet()){
                if(commands.containsKey(entry.getKey())) {
                    for (Link link : entry.getValue()) {
                        HashMap<String, Object> cpy = new HashMap<String, Object>(commands.get(entry.getKey()));
                        cpy.put("tag",entry.getKey());
                        if (link.rule != null) cpy.put("rule", link.rule);
                        if (link.msgData != null) cpy.put("msgData", link.msgData);
                        ar.add(cpy);
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
        return commandLinks.getLinksList();
    }
    public static void resetLinks(ArrayList<Map<String,Object>> newData){
        commandLinks.clear();
        for(Map<String,Object> data : newData){
            addEventLink(data);
        }
    }
    private static void callByTag(PluginProxyInterface proxy, String source){
        ArrayList<HashMap<String,Object>> commandsInfo=getCommandsMatch(source);
        for(Map<String,Object> command : commandsInfo) {
            String tag = (String) command.get("tag");
            proxy.sendMessage(tag, new HashMap<String, Object>() {{
                    if (command.containsKey("msgData"))
                        put("msgData", command.get("msgData"));
            }});
        }
    }
    public static void initialize(PluginProxyInterface proxy){
        proxy.addMessageListener("core:add-command", (sender, tag, data) -> addCommand(sender,(Map<String,Object>) data) );
        proxy.addMessageListener("core:remove-command", (sender, tag, data) -> removeCommand(sender, (String) data) );
        proxy.addMessageListener("core:add-event", (sender, tag, data) -> addEvent(sender,(Map<String,Object>) data) );
        proxy.addMessageListener("core:remove-event", (sender, tag, data) -> removeEvent(sender,(String) data) );
        proxy.addMessageListener("core:set-event-link", (sender, tag, data) -> addEventLink((Map<String,Object>) data) );
        proxy.addMessageListener("core:remove-event-link", (sender, tag, data) -> removeEventLink((Map<String,Object>) data) );
        proxy.addMessageListener("core:get-commands-match", (sender, tag, dat) -> {
            Map<String,Object> data=(Map<String,Object>) dat;
            String eventName=(String) data.getOrDefault("eventName",null);
            Integer seq=(Integer) data.getOrDefault("seq",null);
            data=new HashMap<>();
            data.put("seq",seq);
            data.put("commands",getCommandsMatch(eventName));
            proxy.sendMessage(sender,data);
        });
        proxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
            Iterator<Map.Entry<String,Map<String,Object>>> i = commands.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String,Map<String,Object>> s = i.next();
                if(s.getValue().get("owner").equals(data))
                    i.remove();
            }
            i = events.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String,Map<String,Object>> s = i.next();
                if(s.getValue().get("owner").equals(data))
                    i.remove();
            }
            callByTag(proxy,"core-events:plugin-unload");
        } );
        proxy.addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
            callByTag(proxy,"core-events:plugin-load");
        } );
        proxy.addMessageListener("core-events:loading-complete", (sender, tag, data) -> {
            Map<String,Object> owner=new HashMap<String,Object>();
            owner.put("owner","core");
            events.put("core-events:loading-complete",owner);
            events.put("core-events:plugin-load",owner);
            events.put("core-events:plugin-unload",owner);
            load();
            callByTag(proxy,"core-events:loading-complete");
        });
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
            out.write(commandLinks.toJSONArray().toString(2));
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
        commandLinks.clear();
        try {
            StringBuilder b=new StringBuilder();
            out.lines().forEach(s -> b.append(s));
            JSONArray array=new JSONArray(b.toString());
            for(Object obj : array) {
                if(!(obj instanceof JSONArray)) continue;
                JSONArray link=(JSONArray) obj;
                if(link.length()<3 || link.getString(0).length()==0 || link.getString(1).length()==0 || link.getString(0).length()==2) continue;
                addEventLink(link.getString(0), link.getString(1), link.getString(2), link.length()>3 ? link.getString(3) : "", false);
            }
            PluginManager.log("Links successfully loaded");
        } catch (Exception e){
            commandLinks=defaultCommandLinks.clone();
            PluginManager.log("Error while loading command links file");
            PluginManager.log(e);
        }
    }
}
