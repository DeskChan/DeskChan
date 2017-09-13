package info.deskchan.weather;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core_utils.TextOperations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;

public class Main implements Plugin {

    private static PluginProxyInterface pluginProxy;

    private static Properties properties;
    public static String getCity(){
        return properties.getProperty("city");
    }
    private WeatherServer server = new YahooServer();
    @Override
    public boolean initialize(PluginProxyInterface pluginProxyIn) {
        pluginProxy=pluginProxyIn;
        properties=new Properties();
        try {
            InputStream ip = Files.newInputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
            properties.load(ip);
            ip.close();
        } catch (Exception e) {
            properties = new Properties();
            properties.setProperty("city","Nowhere");
            log("Cannot find file: " + pluginProxy.getDataDirPath().resolve("config.properties"));
        }

        pluginProxy.addMessageListener("weather:update-city",(sender, tag, data) -> {
            String c=(String) ((Map<String,Object>) data).get("city");
            if(c.length()<2) {
                pluginProxy.sendMessage("gui:show-notification", new HashMap<String,Object>(){{
                    put("name",getString("error"));
                    put("text",getString("info.no-city"));
                }});
            } else properties.setProperty("city",c);
            server.drop();
            updateOptionsTab();
            saveOptions();
            System.out.println(server.getNow()+" "+getCity());
        });
        pluginProxy.sendMessage("core:add-command", TextOperations.toMap("tag: \"weather:say-weather\""));
        String[] v=new String[]{ "", "", " сейчас", "", " сегодня", "0", " завтра", "1", " послезавтра", "2"};
        for(int i=0;i<5;i++) {
            Map m=new HashMap<String, String>();
            m.put("eventName", "speech:get");
            m.put("commandName", "weather:say-weather");
            m.put("rule", "погода" + v[i * 2]);
            if (i != 1) m.put("msgData", v[1 + i * 2]);
            pluginProxy.sendMessage("core:set-event-link",  m);
        }
        pluginProxy.addMessageListener("weather:say-weather",(sender, tag, data) -> {
            Map<String, Object> say = new HashMap<>();
            say.put("priority", 2000);
            say.put("skippable", false);
            Object value=((Map<String, Object>) data).getOrDefault("msgData",null);
            if (value == null ||
               (value instanceof String && ((String) value).length()==0) ||
               (value instanceof Map && ((Map) value).size()==0)) {
                say.put("text", getString("now")+" "+server.getNow().toString()+", "+Main.getString("lastUpdate")+": "+server.getLastUpdate());
            } else if (value.equals("all")) {
                List<String> b = new ArrayList<>(11);
                b.add(getString("now") + " - " + server.getNow().toString());
                for (int i = 0; i < 10; i++) {
                    b.add(server.getByDay(i).toString());
                }
                b.add(Main.getString("lastUpdate")+": "+server.getLastUpdate());
                say.put("text", b);
            } else {
                Integer num = 0;
                if(value instanceof Number) num=((Number)value).intValue();
                else if(value instanceof String){
                    String c = (String) value;
                    try {
                        num = Integer.valueOf(c);
                    } catch (Exception e) { }
                }
                if(num>=server.getDaysLimit()){
                    say.put("text", "Ой, прости, этот день будет слишком нескоро. Я не знаю, какая будет погода.");
                } else {
                    say.put("text", server.getByDay(num).toString() + ", " + Main.getString("lastUpdate") + ": " + server.getLastUpdate());
                }
            }
            pluginProxy.sendMessage("DeskChan:say",say);
        });
        pluginProxy.addMessageListener("talk:reject-quote",(sender, tag, dat) -> {
            HashMap<String, Object> data = (HashMap<String, Object>) dat;
            ArrayList<HashMap<String, Object>> list = (ArrayList<HashMap<String, Object>>) data.getOrDefault("quotes", null);
            HashMap<String, Object> ret = new HashMap<>();
            ret.put("seq", data.get("seq"));
            ArrayList<HashMap<String, Object>> quotes_list = new ArrayList<>();
            ret.put("quotes",quotes_list);
            if (list != null) {
                TimeForecast now=server.getNow();
                for (HashMap<String, Object> entry : list) {
                    List<String> types = (List<String>) entry.get("weather");
                    if(types==null) continue;
                    if(now==null){
                        quotes_list.add(entry);
                        continue;
                    }
                    for(String type : types){
                        if(!isWeatherMatch(type,server.getNow().weather,server.getNow().temp)){
                            quotes_list.add(entry);
                            break;
                        }
                    }
                }
            }
            pluginProxy.sendMessage(sender,ret);
        });
        setupOptionsTab();
        return true;
    }
    String checkLocationResult(){
        String ret=server.checkLocation();
        if(ret.equals("1")) return getString("info.lost-server");
        if(ret.equals("2") || ret.equals("3")) return getString("incorrect");
        return ret;
    }
    void setupOptionsTab() {
        pluginProxy.sendMessage("gui:setup-options-submenu", new HashMap<String, Object>() {{
            put("name", getString("options"));
            put("msgTag", "weather:update-city");
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "city");
                put("type", "TextField");
                put("label", getString("city"));
                put("value", getCity());
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "check");
                put("type", "Label");
                put("value", checkLocationResult());
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", getString("info.check"));
            }});
            put("controls", list);
        }});
    }
    void updateOptionsTab() {
        pluginProxy.sendMessage("gui:update-options-submenu", new HashMap<String, Object>() {{
            put("name", getString("options"));
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "check");
                put("value", checkLocationResult());
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "city");
                put("value", getCity());
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
    @Override
    public void unload(){

    }
    public boolean isWeatherMatch(String match, String current, Temperature temp){
        if(match.equals("cold"))
            return (temp.getInt()<-9);
        if(match.equals("hot"))
            return (temp.getInt()>27);
        if(match.equals("good"))
            return (current.equals("clear") || current.equals("cloudy"));
        if(match.equals("bad"))
            return (!current.equals("clear") && !current.equals("cloudy"));
        return current.equals(match);
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }

    public static String getString(String text){
        return pluginProxy.getString(text);
    }
}
