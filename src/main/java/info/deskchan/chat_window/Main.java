package info.deskchan.chat_window;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;


public class Main implements Plugin {
    private static PluginProxyInterface pluginProxy;
    private static Properties properties;

    public String characterName;
    public String userName;

    private class ChatPhrase{
        public final String text;
        public final int sender;
        public final Date date;

        public ChatPhrase(String text,int sender){
            this.text=text;
            this.sender=sender;
            date=new Date();
        }
        public HashMap<String,Object> toMap(){
            HashMap<String,Object> map=new HashMap<>();
            String color=null;
            String senderName=null;
            switch(sender){
                case 0: {
                    color="#F00";
                    senderName=characterName;
                } break;
                case 1: {
                    color="#00F";
                    senderName=userName;
                } break;
                case 2: color="#888"; break;
            }
            map.put("text","("+new SimpleDateFormat("HH:mm:ss").format(date)+") "+(senderName!=null ? "["+senderName+"]: " : "")+text+"\n");
            map.put("color",color);
            return map;
        }
    }

    private boolean chatIsOpened=false;

    private LinkedList<ChatPhrase> history;
    private int logLength=1;
    private ArrayList<HashMap<String,Object>> historyToChat(){
        ArrayList<HashMap<String,Object>> ret=new ArrayList<>();
        List<ChatPhrase> list=history.subList(Math.max(history.size() - logLength, 0), history.size());
        if(list.size()==0){
            ret.add(new ChatPhrase("История сообщений пуста",2).toMap());
            return ret;
        }
        for(ChatPhrase phrase : list){
            ret.add(phrase.toMap());
        }
        return ret;
    }

    @Override
    public boolean initialize(PluginProxyInterface newPluginProxy) {
        pluginProxy=newPluginProxy;
        log("setup chat window started");
        history=new LinkedList<>();
        properties=new Properties();
        try {
            InputStream ip = Files.newInputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
            properties.load(ip);
            ip.close();
        } catch (Exception e) {
            properties = new Properties();
            properties.setProperty("length","10");
            properties.setProperty("fixer","true");
            log("Cannot find file: " + pluginProxy.getDataDirPath().resolve("config.properties"));
        }
        logLength = Integer.parseInt(properties.getProperty("length"));

        pluginProxy.setResourceBundle("info/deskchan/chat_window/strings");

        characterName = "DeskChan";
        userName = pluginProxy.getString("default-username");

        pluginProxy.addMessageListener("chat:setup", (sender, tag, data) -> {
            chatIsOpened=true;
            setupChat();
        });
        pluginProxy.addMessageListener("chat:closed", (sender, tag, data) -> {
            chatIsOpened=false;
        });
        pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("chat.open"));
            put("msgTag", "chat:setup");
        }});
        pluginProxy.addMessageListener("DeskChan:say", (sender, tag, data) -> {
            String text;
            if(data instanceof Map){
                Map m=(Map) data;
                if(m.containsKey("text"))
                    text=(String) m.get("text");
                else if(m.containsKey("msgData"))
                    text=(String) m.get("msgData");
                else text="";
            } else {
                text=data.toString();
            }
            Map<String, Object> delayData = new HashMap<>();
            delayData.put("delay", 1);
            pluginProxy.sendMessage("core-utils:notify-after-delay", delayData, (s, d) -> {
                history.add(new ChatPhrase(text,0));
                if(!chatIsOpened) return;
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
            Map<String,Object> data=(HashMap<String,Object>) dat;
            String value=(String) data.getOrDefault("value", "");
            history.add(new ChatPhrase(value,1));
            if(properties.getProperty("fixer").equals("true")){
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
            Map<String,Object> data=(Map<String,Object>) dat;
            properties.setProperty("fixer",data.get("fixer").toString());
            properties.setProperty("length",data.get("length").toString());
            logLength = Integer.parseInt(properties.getProperty("length"));
            setupOptions();
            saveOptions();
        });
        pluginProxy.addMessageListener("talk:character-updated", (sender, tag, data) -> {
            Map map=(Map<String,Object>) data;
            characterName = (String) map.get("name");
            map = (Map<String,List<String>>) map.get("tags");
            if(map!=null){
                Object un=map.get("usernames");
                if(un==null) return;
                if(un instanceof String){
                    userName = (String) un;
                } else {
                    List list = (List<String>) un;
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
            LinkedList<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "name");
                put("type", "TextField");
                put("width",400d);
                put("enterTag","chat:user-said");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "textname");
                put("type", "CustomizableTextArea");
                put("width",400d);
                put("height",200d);
                put("value",historyToChat());
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
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "fixer");
                put("type", "CheckBox");
                put("label", pluginProxy.getString("fix-layout"));
                put("value", properties.getProperty("fixer").equals("true"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "length");
                put("type", "Spinner");
                put("min", 1);
                put("max", 20000);
                put("label", pluginProxy.getString("log-length"));
                put("value", Integer.parseInt(properties.getProperty("length")));
            }});
            put("controls", list);
        }});
    }
    void saveOptions(){
        try {
            OutputStream ip = Files.newOutputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
            properties.store(ip, "config fot weather plugin");
            ip.close();
        } catch (IOException e) {
            log(e);
        }
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
