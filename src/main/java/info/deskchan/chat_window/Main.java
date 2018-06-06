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

    /** Single phrase in chat. **/
    private class ChatPhrase{
        public final String text;
        public final int sender;
        public final Date date;

        public ChatPhrase(String text,int sender){
            this.text = text;
            this.sender = sender;
            date = new Date();
        }
        public Map<String,Object> getNameMap(){
            Map<String,Object> map = new HashMap<>();
            String color = null;
            String senderName = null;
            String font = null;
            String dateString = "(" + new SimpleDateFormat("HH:mm:ss").format(date) + ") ";
            switch(sender){
                case 0: {  // DeskChan
                    color = properties.getString("deskchan-color", "#F00");
                    font  = properties.getString("deskchan-font");
                    senderName = characterName;
                } break;
                case 1: {  // User
                    color = properties.getString("user-color", "#00A");
                    font  = properties.getString("user-font");
                    senderName = userName;
                } break;
                case 2: {  // Technical messages
                    color = "#888";
                    dateString = "";
                } break;
            }
            map.put("text", dateString + (senderName!=null ? "["+senderName+"]: " : ""));
            map.put("color", color);
            map.put("id", "name");
            if(font != null) {
                map.put("font", font);
            }
            return map;
        }
        public Map<String,Object> getTextMap(){
            Map<String,Object> map = new HashMap<>();
            map.put("text", text + "\n");
            map.put("id", "text");
            return map;
        }
    }

    private boolean chatIsOpened = false;

    private LinkedList<ChatPhrase> history = new LinkedList<>();;
    private int logLength = 1;
    private String currentQuery = "";

    private List<Map> historyToChat(){
        List<Map> ret = new ArrayList<>();
        List<ChatPhrase> list = history.subList(Math.max(history.size() - logLength, 0), history.size());
        if(list.size() == 0){
            ret.add(new ChatPhrase("История сообщений пуста", 2).getNameMap());
            return ret;
        }
        for(ChatPhrase phrase : list){
            ret.add(phrase.getNameMap());
            ret.add(phrase.getTextMap());
        }
        return ret;
    }

    private List<String> resend = new ArrayList<>();

    @Override
    public boolean initialize(PluginProxyInterface newPluginProxy) {
        pluginProxy = newPluginProxy;
        log("setup chat window started");
        pluginProxy.setConfigField("name", pluginProxy.getString("plugin-name"));

        // setting default properties
        properties = pluginProxy.getProperties();
        properties.load();
        properties.putIfHasNot("length", 10);
        properties.putIfHasNot("fixer", true);
        properties.putIfHasNot("user-color", "#00A");
        properties.putIfHasNot("deskchan-color", "#F00");

        logLength = properties.getInteger("length");

        characterName = "DeskChan";
        userName = pluginProxy.getString("default-username");

        /* Open chat request.
        * Public message
        * Params: None
        * Returns: None */
        pluginProxy.addMessageListener("chat:open", (sender, tag, data) -> {
            chatIsOpened = true;
            pluginProxy.sendMessage("DeskChan:request-say", "START_DIALOG");
            pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
                put("id", "chat");
                put("action", "show");
            }});
        });

        pluginProxy.sendMessage("core:add-command", new HashMap(){{
            put("tag", "chat:open");
        }});
        pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>(){{
            put("eventName", "gui:keyboard-handle");
            put("commandName", "chat:open");
            put("rule", "ALT+C");
        }});

        /* Chat has been closed through GUI. */
        pluginProxy.addMessageListener("chat:closed", (sender, tag, data) -> {
            chatIsOpened = false;
        });

        /* Updated textfield input. */
        pluginProxy.addMessageListener("chat:update-textfield", (sender, tag, data) -> {
            currentQuery = data.toString();
        });

        /* Someone made request to clear all messages from chat. We're clearing it. */
        pluginProxy.addMessageListener("chat:clear", (sender, tag, data) -> {
            history.clear();
            setupChat();
        });

        /* Someone made request to change user phrases color in chat.
        * Public message
        * Params: color:String? or null to reset
        * Returns: None */
        pluginProxy.addMessageListener("chat:set-user-color", (sender, tag, data) -> {
            properties.put("user-color", data.toString());
            setupChat();
        });

        /* Someone made request to change user DeskChan color in chat.
        * Public message
        * Params: color:String? or null to reset
        * Returns: None */
        pluginProxy.addMessageListener("chat:set-deskchan-color", (sender, tag, data) -> {
            properties.putIfNotNull("deskchan-color", data.toString());
            setupChat();
        });

        /* Listening all DeskChan speech to show it in chat. */
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
                text = data.toString();
            }

            // very small timer to run this func in other thread
            pluginProxy.setTimer(20, (s, d) -> {
                history.add(new ChatPhrase(text, 0));
                if (!chatIsOpened) return;

                pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
                    put("id", "chat");
                    put("action", "update");
                    LinkedList<HashMap<String, Object>> list = new LinkedList<>();
                    list.add(new HashMap<String, Object>() {{
                        put("id", "textarea");
                        put("value", historyToChat());
                    }});
                    put("controls", list);
                }});
            });
        });

        /* Listening all user speech to show it in chat.
        * Technical message
        * Params: value: String! - user speech text
        * Returns: None */
        pluginProxy.addMessageListener("chat:user-said", (sender, tag, dat) -> {
            String value;
            Map<String, Object> data;
            if (dat instanceof Map){
                data = (Map) dat;
                value = (String) data.getOrDefault("value", "");
            } else {
                if (dat != null)
                    value = (String) dat;
                else
                    value = currentQuery;
                data = new HashMap<>();
                data.put("value", value);
            }
            currentQuery = "";

            history.add(new ChatPhrase(value, 1));

            pluginProxy.sendMessage("DeskChan:user-said",data);
            setupChat();
        });

        pluginProxy.sendMessage("core:register-alternative",
                new HashMap<String, Object>() {{
                    put("srcTag", "DeskChan:user-said");
                    put("dstTag", "chat:change-layout-and-resend");
                    put("priority", 5);
                }}
        );

        // if user missed layout, we're trying to fix it and resend user speech again
        pluginProxy.addMessageListener("chat:change-layout-and-resend", (sender, tag, dat) -> {
            Map data = (Map) dat;
            if(properties.getBoolean("fixer", true)){
                String query = (String) data.get("value");
                if (resend.contains(query)){
                    resend.remove(query);
                } else {
                    resend.add(query);
                    String translate = FixLayout.fixRussianEnglish(query);
                    if (!translate.equals(query)) {
                        history.add(new ChatPhrase(pluginProxy.getString("wrong-layout") + " " + translate, 2));
                        Map<String, Object> cl = new HashMap<>(data);
                        cl.put("value", translate);
                        pluginProxy.sendMessage("DeskChan:user-said", cl);
                    }
                    return;
                }
            }
            pluginProxy.sendMessage("DeskChan:say#chat:change-layout-and-resend", dat);
        });

        /* Saving options changed in options window.
        * Public message
        * Params: fixer: Boolean? - turn layout fixed on/off
        *         length: Int? - history length
        *         user-color: String? - user color
        *         deskchan-color: String? - deskchan color
        *         user-font: String? - user font in inner format
        *         deskchan-fontr: String? - deskchan font in inner format
        * Returns: None */
        pluginProxy.addMessageListener("chat:save-options", (sender, tag, dat) -> {
            try {
                Map data = (Map) dat;
                properties.putIfNotNull("fixer", data.get("fixer"));
                properties.putIfNotNull("length", data.get("length"));
                properties.putIfNotNull("user-color", data.get("user-color"));
                properties.putIfNotNull("deskchan-color", data.get("deskchan-color"));
                properties.putIfNotNull("user-font", data.get("user-font"));
                properties.putIfNotNull("deskchan-font", data.get("deskchan-font"));

                logLength = properties.getInteger("length", logLength);

                saveOptions();
                setupChat();
                setupOptions();
            } catch (Exception e){

            }
        });

        /* Character was updated, so we changing his name and username in chat. */
        pluginProxy.addMessageListener("talk:character-updated", (sender, tag, data) -> {
            onCharacterUpdate((Map) data);
        });
        pluginProxy.sendMessage("talk:get-preset", null, (sender, data) -> {
            onCharacterUpdate((Map) data);
        });

        /* Registering "Open chat" button in menu. */
        pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>() {{
            put("eventName", "gui:menu-action");
            put("commandName", "chat:open");
            put("rule", pluginProxy.getString("chat.open"));
        }});

        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
            LinkedList<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "textarea");
                put("type", "CustomizableTextArea");
                put("width", 500d);
                put("height", 300d);
                put("value", historyToChat());
            }});
            List input = new ArrayList();
            input.add(new HashMap<String, Object>() {{
                put("id", "input");
                put("type", "TextField");
                put("onChangeTag","chat:update-textfield");
                put("enterTag","chat:user-said");
            }});
            input.add(new HashMap<String, Object>() {{
                put("id", "enter");
                put("type", "Button");
                put("msgTag","chat:user-said");
                put("value", pluginProxy.getString("send"));
            }});
            input.add(new HashMap<String, Object>() {{
                put("id", "options");
                put("type", "Button");
                put("value", "⚙");
                put("dstPanel", pluginProxy.getId() + "-options");
            }});
            input.add(new HashMap<String, Object>() {{
                put("id", "help");
                put("type", "Button");
                put("value", "?");
                put("msgTag", "DeskChan:commands-list");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "input-line");
                put("elements", input);
            }});
            put("controls", list);
            put("name", pluginProxy.getString("chat"));
            put("id", "chat");
            put("type", "window");
            put("onClose","chat:closed");
            put("action", "set");
        }});

        setupOptions();
        log("setup chat window completed");

        return true;
    }

    void onCharacterUpdate(Map<String, Object> map){
        String name = map.get("name").toString();
        if (name != null)
            characterName = name;
        else
            characterName = "DeskChan";
        map = (Map) map.get("tags");
        if(map != null){
            Object un = map.get("usernames");
            if(un == null) return;
            if(un instanceof String){
                userName = (String) un;
            } else {
                Collection list = (Collection) un;
                if(list != null && list.size() > 0)
                    userName = (String) list.iterator().next();
            }
        }
    }

    /** Chat drawing. **/
    void setupChat() {
        if(!chatIsOpened) return;
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
            put("id", "chat");
            put("action", "update");
            LinkedList<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "options");
                put("value", pluginProxy.getString("options"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "input");
                put("value", "");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "textarea");
                put("value", historyToChat());
            }});
            put("controls", list);
        }});
    }

    /** Options window drawing. **/
    void setupOptions(){
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
            put("id", "options");
            put("name", pluginProxy.getString("options"));
            put("msgTag", "chat:save-options");
            put("type", "submenu");
            put("action", "set");
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

    static void log(String text) { pluginProxy.log(text); }

    static void log(Throwable e) { pluginProxy.log(e);    }
}
