package info.deskchan.core_utils;

import info.deskchan.core.PluginProxyInterface;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Map;

public class Downloader {
    
    public static void initialize(PluginProxyInterface pluginProxy) {
        if (pluginProxy.getProperties().getString("default_download_path", null) == null) {
            pluginProxy.getProperties().set("default_download_path", pluginProxy.getDataDirPath().toString() + "Downloader");
        }
        
        pluginProxy.sendMessage("core:add-command", new HashMap<String, Object>() {{
            put("tag", "core-utils:download");
            put("info", pluginProxy.getString("downloader.download"));
            put("msgInfo", new HashMap<String, String>() {{
                put("url", pluginProxy.getString("downloader.downloader.url"));
                put("path", pluginProxy.getString("downloader.downloader.path"));
                put("name", pluginProxy.getString("downloader.downloader.name"));
            }});
        }});

        pluginProxy.sendMessage("core:add-command", new HashMap<String, Object>() {{
            put("tag", "core-utils:set-default-download-path");
            put("info", pluginProxy.getString("downloader.set_default_path"));
            put("msgInfo", new HashMap<String, String>(){{
                put("path", pluginProxy.getString("downloader.set_default_path.path"));
            }})
        }});
        
        pluginProxy.addMessageListener("core-utils:download", (sender, tag, data) -> {
            try {
                Map<String, Object> mapData = (Map) data;
                String urlString = mapData.get("url").toString();
                URL url = new URL(urlString);
                String path = mapData.containsKey("path") ? mapData.get("path").toString() : pluginProxy.getProperties().get("default_download_path");
                String filename = mapData.containsKey("filename") ? mapData.get("filename").toString() : urlToFilename(urlString);

                if (filename == null) {
                    pluginProxy.log("Could not make filename from url " + urlString);
                    return;
                }
                
                path = path + "/" + filename;
                
                try (ReadableByteChannel in = Channels.newChannel(url.openStream());
                     FileOutputStream out = new FileOutputStream(path))
                    {
                        out.getChannel().transferFrom(in, 0, Long.MAX_VALUE);
                }
            } catch (Exception e) {
                pluginProxy.log(e);
            }
        });

        pluginProxy.addMessageListener("core-utils:set-default-download-path", (sender, tag, data) -> {
                try {
                    pluginProxy.getProperties().set("default_download_path", ((Map) data).get("path").toString());
                } catch (Exception e) {
                    pluginProxy.log(e);
                }
            });        
    }

    private static String urlToFilename(String url) {
        String filename;
        int lastSlashIndex = url.lastIndexOf("/");
        if (lastSlashIndex >= 0 && lastSlashIndex < url.length()+1)
            filename = url.substring(lastSlashIndex+1);
        else 
            return null;

        return  filename;
    }
}
