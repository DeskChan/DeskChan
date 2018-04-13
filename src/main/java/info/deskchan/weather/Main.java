package info.deskchan.weather;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProperties;
import info.deskchan.core.PluginProxyInterface;

import java.util.*;

public class Main implements Plugin {

    private static PluginProxyInterface pluginProxy;

    private WeatherServer server;

    public static String getCity(){
        return pluginProxy.getProperties().getString("city");
    }

    public static Main instance;

    @Override
    public boolean initialize(PluginProxyInterface pluginProxyIn) {
        pluginProxy = pluginProxyIn;
        instance = this;

        PluginProperties properties = pluginProxy.getProperties();
        properties.load();
        properties.putIfHasNot("city", "Nowhere");

        server = new YahooServer();

        pluginProxy.addMessageListener("weather:update-city",(sender, tag, data) -> {
            String city = ((Map) data).get("city").toString();
            if(city.length() < 2) {
                pluginProxy.sendMessage("gui:show-notification", new HashMap<String,Object>(){{
                    put("name",getString("error"));
                    put("text",getString("info.no-city"));
                }});
            } else properties.put("city", city);
            new Thread(){
                public void run(){
                    server.drop();
                    checkLocation();
                    saveOptions();
                }
            }.start();
        });

        pluginProxy.sendMessage("core:add-command", new HashMap(){{
            put("tag", "weather:say-weather");
        }});

        Map m = new HashMap<String, String>();
        m.put("eventName", "speech:get");
        m.put("commandName", "weather:say-weather");
        m.put("rule", "погода {date:DateTime}");
        pluginProxy.sendMessage("core:set-event-link",  m);

        pluginProxy.addMessageListener("weather:say-weather",(sender, tag, data) -> {
            Map<String, Object> say = new HashMap<>();
            say.put("priority", 2000);
            say.put("skippable", false);

            Object value = ((Map) data).get("date");
            if (value == null ||
               (value instanceof String && ((String) value).length()==0) ||
               (value instanceof Map && ((Map) value).size()==0)) {
                say.put("text", getString("now")+" "+server.getNow().toString()+", "+Main.getString("lastUpdate")+": "+server.getLastUpdate());
            } /*else if (value.equals("all")) {
                List<String> b = new ArrayList<>(11);
                b.add(getString("now") + " - " + server.getNow().toString());
                for (int i = 0; i < 10; i++) {
                    b.add(server.getByDay(i).toString());
                }
                b.add(Main.getString("lastUpdate")+": "+server.getLastUpdate());
                say.put("text", b);
            } */ else {
                Long num = 0L;
                if(value instanceof Number) num = ((Number)value).longValue();
                else if(value instanceof String){
                    String c = (String) value;
                    try {
                        num = Long.valueOf(c);
                    } catch (Exception e) { }
                }
                Calendar calendar = Calendar.getInstance(), now = Calendar.getInstance();
                calendar.setTimeInMillis(num);
                if (now.getTimeInMillis() > calendar.getTimeInMillis() &&
                        calendar.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)){
                    say.put("text", "Я пока не могу сказать, какая погода была в прошлом. И вообще, надо смотреть в будущее.");
                } else if (Math.abs(now.getTimeInMillis() - calendar.getTimeInMillis()) < 3600000){
                    say.put("text", getString("now")+" "+server.getNow().toString()+", "+Main.getString("lastUpdate")+": "+server.getLastUpdate());
                } else {
                    int days = 0;
                    while (calendar.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                        days++;
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                    }
                    if (days >= server.getDaysLimit()) {
                        say.put("text", "Ой, прости, этот день будет слишком нескоро. Я не знаю, какая будет погода.");
                    } else {
                        say.put("text", server.getByDay(days).toString() + ", " + Main.getString("lastUpdate") + ": " + server.getLastUpdate());
                    }
                }
            }
            pluginProxy.sendMessage("DeskChan:say",say);
        });

        pluginProxy.addMessageListener("talk:reject-quote",(sender, tag, data) -> {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            List<Map<String, Object>> quotes_list = new ArrayList<>();
            if (list != null) {
                TimeForecast now = server.getNow();
                if (now != null) {
                    for (Map<String, Object> entry : list) {
                        Collection<String> types = (Collection) entry.get("weather");
                        if (types == null) continue;
                        if (now == null) {
                            quotes_list.add(entry);
                            continue;
                        }
                        for (String type : types) {
                            if (!isWeatherMatch(type, server.getNow().weather, server.getNow().temp)) {
                                quotes_list.add(entry);
                                break;
                            }
                        }
                    }
                }
            }
            pluginProxy.sendMessage(sender, quotes_list);
        });

        setupOptionsTab();
        new Thread(){
            public void run(){
                checkLocation();
                server.getNow();
            }
        }.start();
        log("Initialization completed");
        return true;
    }

   void checkLocation(){
        String ret = server.checkLocation();
        updateOptionsTab(ret != null ? ret : getString("error"));
    }

    void setupOptionsTab() {
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
            put("name", getString("options"));
            put("id", "options");
            put("msgTag", "weather:update-city");
            put("action", "set");
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "city");
                put("type", "TextField");
                put("label", getString("city"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "check");
                put("type", "Label");
                put("label", getString("error"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", getString("info.check"));
            }});
            put("controls", list);
        }});
    }

    void updateOptionsTab(String locationResult) {
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
            put("id", "options");
            put("name", getString("options"));
            put("action", "update");
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "check");
                put("value", locationResult);
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "city");
                put("value", getCity());
            }});
            put("controls", list);
        }});
    }

    void saveOptions(){
       pluginProxy.getProperties().save();
    }

    @Override
    public void unload(){ }

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
