package info.deskchan.telegram_api;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Main implements Plugin {
    private static PluginProxy pluginProxy;

    @Override
    public boolean initialize(PluginProxy newPluginProxy) {
        pluginProxy = newPluginProxy;
        log("loading api");
        pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("start"));
            put("msgTag", "telegram:start");
        }});
        pluginProxy.addMessageListener("telegram:start", (sender, tag, data) -> {
            App.Start();
        });
        pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("stop"));
            put("msgTag", "telegram:stop");
        }});
        pluginProxy.addMessageListener("telegram:stop", (sender, tag, data) -> {
            App.Stop();
        });
        pluginProxy.sendMessage("talk:add-reciever",new HashMap<String, Object>() {{
            //put("tag", "telegram:send");
        }});
        pluginProxy.addMessageListener("telegram:send", (sender, tag, data) -> {
            Map<String, Object> da = (Map<String, Object>) data;
            App.Send((String)da.getOrDefault("text",null));
        });
        App.Start();
        log("api loaded");
        return true;
    }

    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }

    public static void sendToProxy(String tag, Map<String, Object> data) {
        pluginProxy.sendMessage(tag, data);
    }

    public static Path getDataDirPath() {
        return pluginProxy.getDataDirPath();
    }

    static PluginProxy getPluginProxy() {
        return pluginProxy;
    }
}
