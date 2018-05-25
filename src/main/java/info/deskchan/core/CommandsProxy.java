package info.deskchan.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class CommandsProxy{
    public static class Link{
        public final String rule;
        public final Object msgData;

        public Link(String rule, Object msgData){
            this.rule = (String) getObjectOrNull(rule);
            Object d = getObjectOrNull(msgData);
            if (d instanceof JSONObject){
                d = ((JSONObject) d).toMap();
            } else if (d instanceof JSONArray){
                d = ((JSONArray) d).toList();
            }
            this.msgData = d;
        }

        public boolean equals(Link link){
            return equals(rule, link.rule) && equals(msgData, link.msgData);
        }

        static Object getObjectOrNull(Object text){
            if(text == null || text.equals("") || text.equals("null"))
                return null;
            return text;
        }
        static boolean equals(Object a, Object b){
            return a == b || (a!=null && a.equals(b));
        }
    }

    /** All events registered in program with all information about them. **/
    private static MatrixMap<String, String, Object> events   = new MatrixMap<>();
    /** All commands registered in program with all information about them. **/
    private static MatrixMap<String, String, Object> commands = new MatrixMap<>();

    /** All command links used by program (including added by user and excluding deleted by user). **/
    private static LinkContainer commandLinks = new LinkContainer();
    /** All command links registered by plugins by default. **/
    private static LinkContainer defaultCommandLinks = new LinkContainer();


    /** Resets links list to default, set by plugins. **/
    public static void reset(){
        commandLinks = defaultCommandLinks.clone();
        sendNotify();
        for(String event : events.map.keySet())
            sendUpdateByEvent(event);
    }


    /** Adds command to list, it will be visible in GUI.
     * All links that using this command will be activated. **/
    private static void addCommand(String sender, Map<String,Object> data){
        String tag = (String) data.getOrDefault("tag", null);
        if(tag == null) {
            PluginManager.log("No tag specified for command to add: " + data);
            return;
        }

        data.remove("tag", tag);
        data.put("owner", sender);
        commands.put(tag, data);

        sendNotify();
        sendUpdateByCommand(tag);
    }

    /** Remove command from list, it will be not visible in GUI.
     * All links that using this command will be deactivated but not removed. **/
    private static void removeCommand(String sender, String tag){
        if (tag == null) {
            PluginManager.log("No name specified for command to remove, recieved null");
            return;
        }
        Map<String, Object> command = commands.get(tag);
        if(sender.equals("core") || command.get("owner").equals(sender)) {
            commands.remove(tag);
            sendNotify();
            sendUpdateByCommand(tag);
        } else
            PluginManager.log("Plugin \""+sender+"\" is not owner of command \""+tag+"\"");
    }

    /** Adds event to list, it will be visible in GUI.
     * All links that using this event will be activated. **/
    private static void addEvent(String sender, Map<String, Object> data){
        String tag = (String) data.get("tag");
        if(tag == null) {
            PluginManager.log("No name specified for event to add: " + data);
            return;
        }

        data.remove("tag", tag);
        data.put("owner", sender);
        events.put(tag, data);
        sendNotify();
        sendUpdateByEvent(tag);
    }

    /** Remove event from list, it will be not visible in GUI.
     * All links that using this event will be deactivated but not removed. **/
    private static void removeEvent(String sender, String tag){
        if (tag == null) {
            PluginManager.log("No name specified for event to remove, recieved null");
            return;
        }
        Map<String,Object> event = events.get(tag);
        if(sender.equals("core") || event.get("owner").equals(sender)) {
            events.remove(tag);
            sendNotify();
            sendUpdateByEvent(tag);
        } else
            PluginManager.log("Plugin \""+sender+"\" is not owner of event \""+tag+"\"");
    }

    /** Adds link to list, it will be visible in GUI, all plugins will be notified about this.
     * It will be added regardless event and command specified may not exist
     * @param eventName Event name
     * @param commandName Command name
     * @param rule Rule (can be null)
     * @param msgData Data (can be null)
     * @param isDefault is true, link will be also added to default list **/
    public static void addEventLink(String eventName, String commandName, String rule, Object msgData, boolean isDefault){
        if(eventName==null)
            PluginManager.log("No event name specified to event link");

        if(commandName==null)
            PluginManager.log("No target command name specified to add event link");

        Link link = new Link(rule, msgData);
        if(isDefault)
            defaultCommandLinks.add(eventName, commandName, link);
        commandLinks.add(eventName, commandName, link);

        sendNotify();
        sendUpdateByEvent(eventName);
    }

    /** Adds link to list, it will be visible in GUI, all plugins will be notified about this.
     * It will be added regardless event and command specified may not exist
     * @param data All {@link #addEventLink(String, String, String, Object, boolean) arguments} stored in Map **/
    public static void addEventLink(Map<String,Object> data){
        addEventLink((String) data.get("eventName"),
                     (String) data.get("commandName"),
                     (String) data.get("rule"),
                              data.get("msgData"),
                     "true".equals(data.getOrDefault("isDefault", "true")));
    }

    /** Remove link from list, all plugins will be notified about this.
     * It will be added regardless event and command specified may not exist
     * @param eventName Event name
     * @param commandName Command name
     * @param rule Rule  **/
    public static void removeEventLink(String eventName, String commandName, String rule){
        commandLinks.remove(eventName, commandName, rule);
        defaultCommandLinks.remove(eventName, commandName, rule);

        sendNotify();
        sendUpdateByEvent(eventName);
    }

    /** Remove all links from list with event and command specified, all plugins will be notified about this.
     * It will be added regardless event and command specified may not exist
     * @param eventName Event name
     * @param commandName Command name  **/
    public static void removeEventLink(String eventName, String commandName){
        commandLinks.remove(eventName, commandName);
        defaultCommandLinks.remove(eventName, commandName);

        sendNotify();
        sendUpdateByEvent(eventName);
    }

    /** Remove link from list, all plugins will be notified about this.
     * @param data All {@link #removeEventLink(String, String, String) arguments} stored in Map **/
    public static void removeEventLink(Map<String, Object> data){
        if(data.containsKey("rule")){
            removeEventLink((String) data.get("eventName"),
                            (String) data.get("commandName"),
                            (String) data.get("rule"));
        } else {
            removeEventLink((String) data.get("eventName"),
                            (String) data.get("commandName"));
        }
    }

    /** Get all links that subscribed to event.
     * @param eventName Event name
     * @return List of links, where every element contains command <b>tag</b>,
     * <b>rule</b> if link have rule and <b>message</b> if link contains message. **/
    public static ArrayList<HashMap<String, Object>> getCommandsMatch(String eventName){
        ArrayList < HashMap <String, Object> > ar = new ArrayList<>();
        if(eventName == null) {
            PluginManager.log("No event name specified for matching command list");
            return ar;
        }

        Map<String, ArrayList<Link>> map = commandLinks.get(eventName);
        if(map == null)
            return ar;

        for(Map.Entry<String, ArrayList<Link>> entry : map.entrySet()) {
            Map command = commands.get(entry.getKey());
            if (command == null) continue;

            for (Link link : entry.getValue()) {
                HashMap<String, Object> copy = new HashMap<>();
                copy.put("tag", entry.getKey());
                if (link.rule != null)    copy.put("rule", link.rule);
                if (link.msgData != null) copy.put("msgData", link.msgData);
                ar.add(copy);
            }
        }
        return ar;
    }

    /** Get list of all commands names. **/
    public static ArrayList<String> getCommandsList(){
        return new ArrayList<>(commands.map.keySet());
    }

    /** Get information about command. **/
    public static Map<String, Object> getCommandInfo(String command){
        return commands.get(command) != null ? new HashMap<>(commands.get(command)) : null;
    }

    /** Get list of all event names. **/
    public static ArrayList<String> getEventsList(){
        return new ArrayList<>(events.map.keySet());
    }

    /** Get information about event. **/
    public static Map<String, Object> getEventInfo(String command){
        return events.get(command) != null ? new HashMap<>(events.get(command)) : null;
    }

    /** Get list of all links. **/
    public static ArrayList<Map<String,Object>> getLinksList(){
        return commandLinks.getLinksList();
    }

    /** Fill links list from map. **/
    public static void setLinks(List<Map<String, Object>> newData){
        commandLinks.clear();

        for(Map<String,Object> data : newData)
            addEventLink(data);

        sendNotify();
        for(String event : events.map.keySet())
            sendUpdateByEvent(event);
    }

    private static PluginProxyInterface proxy = null;

    /** Call all commands that subscribed to <b>source</b> with data specified. **/
    private static void callByTag(String source){
        for(Map<String, Object> command : getCommandsMatch(source)) {
            String tag = (String) command.get("tag");
            Object msgData = command.get("msgData");
            if (msgData != null) {
                if (!(msgData instanceof Map)) {
                    msgData = new HashMap<>();
                    ((Map) msgData).put("msgData", command.get("msgData"));
                }
            }
            proxy.sendMessage(tag, msgData);
        }
    }

    /** Notify about links list update. **/
    private static void sendNotify(){
        proxy.sendMessage("core:update-links", null);
    }

    /** Send updated list of commands subscribed to event to plugin.
     * @param eventName Event name
     */
    private static void sendUpdateByEvent(String eventName){
        proxy.sendMessage("core:update-links:"+eventName, getCommandsMatch(eventName));
    }

    /** Send updated list of commands subscribed to event to plugin.
     * @param commandName Command name
     */
    private static void sendUpdateByCommand(String commandName){
        Set<String> toUpdate = new HashSet<>();
        for(MatrixMap.Entry entry : commandLinks.entrySet()){
            if(entry.key2.equals(commandName))
                toUpdate.add((String) entry.key1);
        }
        for(String event : toUpdate){
            proxy.sendMessage("core:update-links:"+event, getCommandsMatch(event));
        }
    }

    public static void initialize(PluginProxyInterface proxyInterface){
        proxy = proxyInterface;

        proxy.addMessageListener("core:add-command",        (sender, tag, data) ->    addCommand(sender, (Map) data)    );
        proxy.addMessageListener("core:remove-command",     (sender, tag, data) -> removeCommand(sender, (String) data) );
        proxy.addMessageListener("core:add-event",          (sender, tag, data) ->      addEvent(sender, (Map) data)    );
        proxy.addMessageListener("core:remove-event",       (sender, tag, data) ->   removeEvent(sender, (String) data) );
        proxy.addMessageListener("core:set-event-link",     (sender, tag, data) ->     addEventLink((Map) data)         );
        proxy.addMessageListener("core:remove-event-link",  (sender, tag, data) ->  removeEventLink((Map) data)         );

        proxy.addMessageListener("core:get-commands-match", (sender, tag, data) -> {
            String eventName = data.toString();
            proxy.sendMessage(sender, getCommandsMatch(eventName));
        });

        proxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
            Iterator i = commands.map.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                Map map = (Map) entry.getValue();
                if (map.get("owner").equals(data))
                    i.remove();
            }
            i = events.map.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                Map map = (Map) entry.getValue();
                if (map.get("owner").equals(data))
                    i.remove();
            }
            callByTag("core-events:plugin-unload");
        } );

        proxy.addMessageListener("core-events:plugin-load", (sender, tag, data) ->
                callByTag("core-events:plugin-load") );

        addEvent("core", new HashMap<String, Object>(){{
            put("tag", "core-events:loading-complete");
            put("info", proxy.getString("loading-complete-info"));
        }});

        proxy.addMessageListener("core-events:loading-complete", (sender, tag, data) -> {
            addEvent("core", new HashMap<String, Object>(){{
                put("tag", "core-events:plugin-load");
                put("info", proxy.getString("plugin-load-info"));
            }});
            addEvent("core", new HashMap<String, Object>(){{
                put("tag", "core-events:plugin-unload");
                put("info", proxy.getString("plugin-unload-info"));
            }});
            load();
            PluginManager.log("Loading completed");
            callByTag("core-events:loading-complete");
        });
    }

    private static final File file = PluginManager.getDataDirPath().resolve("core").resolve("links").toFile();

    /** Save current links configuration to file **/
    public static void save(){
        OutputStreamWriter writer;
        try {
           writer=new OutputStreamWriter(new FileOutputStream(file.toString()), "UTF-8");
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

    /** Load links configuration from file **/
    public static void load(){
        InputStreamReader reader;
        try {
            reader=new InputStreamReader(new FileInputStream(file.toString()), "UTF-8");
        } catch(Exception e){
            PluginManager.log("Links file is not found, using default links");
            return;
        }
        BufferedReader out=new BufferedReader(reader);
        commandLinks.clear();
        try {
            StringBuilder b = new StringBuilder();
            out.lines().forEach(b::append);
            JSONArray array = new JSONArray(b.toString());
            for(Object obj : array) {
                if(!(obj instanceof JSONArray)) continue;
                JSONArray link = (JSONArray) obj;
                if(link.length()<3 || link.getString(0).length()==0 || link.getString(1).length()==0 || link.getString(0).length()==2) continue;
                addEventLink(link.getString(0), link.getString(1), link.getString(2), link.length()>3 ? link.get(3) : "", false);
            }
            PluginManager.log("Links successfully loaded");
        } catch (Exception e){
            commandLinks = defaultCommandLinks.clone();
            PluginManager.log("Error while loading command links file");
            PluginManager.log(e);
        }
    }


    // -- just helpful classes, don't mind about it -- //


    private static class MatrixMap<TK1, TK2, TV> {

        public class Entry<eTK1, eTK2, eTV>{
            public final eTK1 key1;
            public final eTK2 key2;
            public final eTV value;
            public Entry(eTK1 key1, eTK2 key2, eTV value){
                this.key1 = key1;
                this.key2 = key2;
                this.value = value;
            }
            public boolean equals(Entry other){
                return key1.equals(other.key1) && key2.equals(other.key2) && value.equals(other.value);
            }
        }

        private Map<TK1, Map<TK2, TV>> map = new HashMap<>();
        private Set<Entry<TK1, TK2, TV>> entrySet = new LinkedHashSet<>();

        public Map<TK2, TV> get(TK1 key){
            return map.get(key);
        }

        public TV get(TK1 key1, TK2 key2){
            try {
                return map.get(key1).get(key2);
            } catch (Exception e){
                return null;
            }
        }

        public Object remove(TK1 key1, TK2 key2){
            try {
                Object val = map.get(key1).remove(key2);
                Iterator<Entry<TK1, TK2, TV>> it = entrySet.iterator();
                while(it.hasNext()){
                    Entry<TK1, TK2, TV> entry = it.next();
                    if(entry.key1.equals(key1) && entry.key2.equals(key2))
                        it.remove();
                }
                return val;
            } catch (Exception e){
                PluginManager.log(e);
                return null;
            }
        }

        public Object remove(TK1 key){
            Object value = map.remove(key);
            Entry[] array = (Entry[]) entrySet.toArray();
            for(Entry entry : array)
                if(entry.key1 == key)
                    entrySet.remove(entry);
            return value;
        }

        public Object put(TK1 key1, TK2 key2, TV value){
            Map submap = map.get(key1);
            if(submap == null)
                map.put(key1, submap = new HashMap<TK2, TV>());

            entrySet.add(new Entry<>(key1, key2, value));
            return submap.put(key2, value);
        }

        public Object put(TK1 key, Object value){
            try {
                Map<TK2, TV> val = (Map) value;
                map.put(key,  val);
                for(Map.Entry<TK2, TV> entry : val.entrySet())
                    entrySet.add(new Entry<>(key, entry.getKey(), entry.getValue()));
                return val;
            } catch (Exception e){
                PluginManager.log(e);
                return null;
            }
        }

        public Set<Entry<TK1, TK2, TV>> entrySet() {
            return entrySet;
        }

        public MatrixMap<TK1, TK2, TV> clone(){
            MatrixMap<TK1, TK2, TV> clonedMap = new MatrixMap<>();
            for(Entry<TK1, TK2, TV> entry : entrySet)
                clonedMap.put(entry.key1, entry.key2, entry.value);

            return clonedMap;
        }

        public void clear(){
            map.clear();
            entrySet.clear();
        }
    }

    private static class LinkContainer{
        // < event, command, links >
        private MatrixMap<String, String, ArrayList<Link>> array = new MatrixMap<>();

        public ArrayList<Link> getLinks(String targetName, String commandName){
            return array.get(targetName, commandName);
        }

        public void add(String eventName, String commandName, Link link){
            ArrayList<Link> links = array.get(eventName, commandName);
            if(links == null) {
                array.put(eventName, commandName, links = new ArrayList<>());
                links.add(link);
                return;
            }
            for(Link at : links)
                if (at.equals(link)) return;

            links.add(link);
        }

        public void remove(String eventName, String commandName){
            array.remove(eventName, commandName);
        }

        public void remove(String eventName, String commandName, String rule){
            try {
                for(Map.Entry<String, ArrayList<Link>> entry : array.get(eventName).entrySet()){
                    Iterator<Link> it = entry.getValue().iterator();
                    while(it.hasNext()) {
                        Link link = it.next();
                        if (entry.getKey().equals(commandName) &&
                                Link.equals(Link.getObjectOrNull(rule), Link.getObjectOrNull(link.rule))){
                            it.remove();
                        }
                    }
                }
            } catch (Exception e){
                PluginManager.log(e);
            }
        }

        public Map<String, ArrayList<Link>> get(String eventName){
            return array.get(eventName);
        }

        public Set< MatrixMap<String, String, ArrayList<Link>>.Entry<String, String, ArrayList<Link>> > entrySet(){
            return array.entrySet();
        }

        public void clear(){
            array.clear();
        }
        public JSONArray toJSONArray(){
            JSONArray array = new JSONArray();
            for(MatrixMap.Entry entry : entrySet()) {
                for(Link link : (ArrayList<Link>) entry.value) {
                    JSONArray ar = new JSONArray();
                    ar.put(entry.key1);
                    ar.put(entry.key2);
                    ar.put(link.rule != null ? link.rule : "");
                    if (link.msgData != null)
                        ar.put(link.msgData);

                    array.put(ar);
                }
            }
            return array;
        }
        public ArrayList<Map<String,Object>> getLinksList(){
            ArrayList< Map<String,Object> > list=new ArrayList<>();
            for(MatrixMap.Entry entry : entrySet()) {
                for(Link link : (ArrayList<Link>) entry.value) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("event", entry.key1);
                    map.put("command", entry.key2);
                    map.put("rule", link.rule);
                    map.put("msgData", link.msgData);
                    list.add(map);
                }
            }
            return list;
        }
        public LinkContainer clone(){
            LinkContainer lc = new LinkContainer();
            lc.array = array.clone();
            return lc;
        }
    }
}
