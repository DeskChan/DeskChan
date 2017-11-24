package info.deskchan.chat_window;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProperties;
import info.deskchan.core.PluginProxyInterface;

import java.text.SimpleDateFormat;
import java.util.*;


public class Main implements Plugin {
    private static PluginProxyInterface pluginProxy;
    private static PluginProperties properties;

    public String characterName;
    public String userName;

    private class ChatPhrase{
        public final String text;
        public final int sender;
        public final Date date;

        public ChatPhrase(String text,int sender){
            this.text = text;
            this.sender = sender;
            date = new Date();
        }
        public HashMap<String,Object> toMap(){
            HashMap<String,Object> map = new HashMap<>();
            String color = null;
            String senderName = "";
            String font = null;
            String dateString = "(" + new SimpleDateFormat("HH:mm:ss").format(date) + ") ";
            switch(sender){
                case 0: {
                    color = properties.getString("deskchan-color");
                    font  = properties.getString("deskchan-font");
                    senderName = characterName;
                } break;
                case 1: {
                    color = properties.getString("user-color");
                    font  = properties.getString("user-font");
                    senderName = userName;
                } break;
                case 2: {
                    color = "#888";
                    dateString = "";
                } break;
            }
            map.put("text", dateString + (senderName!=null ? "["+senderName+"]: " : "") + text + "\n");
            map.put("color", color);
            if(font != null) {
                map.put("font", font);
            }
            return map;
        }
    }

    private boolean chatIsOpened = false;

    private LinkedList<ChatPhrase> history;
    private int logLength = 1;
    private ArrayList<HashMap<String,Object>> historyToChat(){
        ArrayList <HashMap<String,Object>> ret = new ArrayList<>();
        List<ChatPhrase> list = history.subList(Math.max(history.size() - logLength, 0), history.size());
        if(list.size() == 0){
            ret.add(new ChatPhrase("История сообщений пуста", 2).toMap());
            return ret;
        }
        for(ChatPhrase phrase : list){
            ret.add(phrase.toMap());
        }
        return ret;
    }

    @Override
    public boolean initialize(PluginProxyInterface newPluginProxy) {
        pluginProxy = newPluginProxy;
        log("setup chat window started");
        history = new LinkedList<>();

        properties = pluginProxy.getProperties();
        properties.load();
        properties.putIfNull("length", 10);
        properties.putIfNull("fixer", true);
        properties.putIfNull("deskchan-color", "red");
        properties.putIfNull("user-color",  "#00A");

        logLength = properties.getInteger("length").intValue();

        characterName = "DeskChan";
        userName = pluginProxy.getString("default-username");

        pluginProxy.addMessageListener("chat:setup", (sender, tag, data) -> {
            chatIsOpened = true;
            setupChat();
        });

        pluginProxy.addMessageListener("chat:closed", (sender, tag, data) -> {
            chatIsOpened = false;
        });

        pluginProxy.addMessageListener("chat:clear", (sender, tag, data) -> {
            history.clear();
            setupChat();
        });

        pluginProxy.addMessageListener("chat:set-user-color", (sender, tag, data) -> {
            properties.put("user-color", data.toString());
            setupChat();
        });

        pluginProxy.addMessageListener("chat:set-deskchan-color", (sender, tag, data) -> {
            properties.put("deskchan-color", data.toString());
            setupChat();
        });

        pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("chat.open"));
            put("msgTag", "chat:setup");
        }});

        pluginProxy.addMessageListener("DeskChan:say", (sender, tag, data) -> {
            String text;
            if(data instanceof Map){
                Map m = (Map) data;
                if(m.containsKey("text"))
                    text = (String) m.get("text");
                else if(m.containsKey("msgData"))
                    text = (String) m.get("msgData");
                else text="";
            } else {
                text=data.toString();
            }

            pluginProxy.setTimer(20, (s, d) -> {
                history.add(new ChatPhrase(text, 0));
                if (!chatIsOpened) return;

                pluginProxy.sendMessage("gui:update-custom-window", new HashMap<String, Object>() {{
                    LinkedList<HashMap<String, Object>> list = new LinkedList<>();
                    list.add(new HashMap<String, Object>() {{
                        put("id", "textname");
                        put("value",historyToChat());
                    }});
                    put("controls", list);
                    put("name",pluginProxy.getString("chat"));
                }});
            });

        });

        pluginProxy.addMessageListener("chat:user-said", (sender, tag, dat) -> {
            Map<String,Object> data = (Map) dat;
            String value = (String) data.getOrDefault("value", "");
            history.add(new ChatPhrase(value, 1));

            if(properties.getBoolean("fixer", true)){
                String translate = FixLayout.fixRussianEnglish(value);
                if (!translate.equals(value)) {
                    history.add(new ChatPhrase(pluginProxy.getString("wrong-layout") + " " + translate, 2));
                    Map<String, Object> cl = new HashMap<>(data);
                    cl.put("value", translate);
                    pluginProxy.sendMessage("DeskChan:user-said", cl);
                }
            }
            pluginProxy.sendMessage("DeskChan:user-said",data);
            setupChat();
        });

        pluginProxy.addMessageListener("chat:options-saved", (sender, tag, dat) -> {
            Map<String,Object> data = (Map) dat;
            properties.put("fixer",  data.get("fixer"));
            properties.put("length", data.get("length"));
            logLength = properties.getInteger("length").intValue();
            properties.put("user-color",     data.get("user-color"));
            properties.put("deskchan-color", data.get("deskchan-color"));
            String font = (String) data.get("user-font");
            if(font != null)
                properties.put("user-font", font);
            font = (String) data.get("deskchan-font");
            if(font != null)
                properties.put("deskchan-font", font);
            setupOptions();
            saveOptions();
            setupChat();
        });

        pluginProxy.addMessageListener("talk:character-updated", (sender, tag, data) -> {
            Map map = (Map) data;
            characterName = (String) map.get("name");
            map = (Map) map.get("tags");
            if(map != null){
                Object un=map.get("usernames");
                if(un == null) return;
                if(un instanceof String){
                    userName = (String) un;
                } else {
                    List list = (List) un;
                    if(list.size()>0)
                        userName = (String) list.get(0);
                }
            }
        });

        log("setup chat window completed");
        setupChat();
        setupOptions();
        return true;
    }

    void setupChat() {
        if(!chatIsOpened) return;
        pluginProxy.sendMessage("gui:show-custom-window", new HashMap<String, Object>() {{
            LinkedList<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "options");
                put("type", "Button");
                put("value", pluginProxy.getString("options"));
                put("msgTag", "gui:show-options-submenu");
                put("msgData", new HashMap<String, Object>(){{
                    put("owner", pluginProxy.getId());
                    put("menu", pluginProxy.getString("options"));
                }});
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "name");
                put("type", "TextField");
                put("width", 400d);
                put("enterTag","chat:user-said");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "textname");
                put("type", "CustomizableTextArea");
                put("width", 400d);
                put("height", 200d);
                put("value", historyToChat());
            }});
            put("controls", list);
            put("name",pluginProxy.getString("chat"));
            put("onClose","chat:closed");
        }});
    }
    void setupOptions(){
        pluginProxy.sendMessage("gui:setup-options-submenu", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("options"));
            put("msgTag", "chat:options-saved");
            List<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "fixer");
                put("type", "CheckBox");
                put("label", pluginProxy.getString("fix-layout"));
                put("value", properties.getBoolean("fixer", true));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "length");
                put("type", "Spinner");
                put("min", 1);
                put("max", 20000);
                put("label", pluginProxy.getString("log-length"));
                put("value", properties.getInteger("length"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "user-color");
                put("type", "ColorPicker");
                put("msgTag", "chat:set-user-color");
                put("label", pluginProxy.getString("user-color"));
                put("value", properties.getString("user-color"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "deskchan-color");
                put("type", "ColorPicker");
                put("msgTag", "chat:set-deskchan-color");
                put("label", pluginProxy.getString("deskchan-color"));
                put("value", properties.getString("deskchan-color"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "user-font");
                put("type", "FontPicker");
                put("label", pluginProxy.getString("user-font"));
                put("value", properties.getString("user-font"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "deskchan-font");
                put("type", "FontPicker");
                put("label", pluginProxy.getString("deskchan-font"));
                put("value", properties.getString("deskchan-font"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "clear");
                put("type", "Button");
                put("msgTag", "chat:clear");
                put("value", pluginProxy.getString("clear"));
            }});
            put("controls", list);
        }});
    }
    void saveOptions(){
        properties.save();
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
