package info.deskchan.core_utils;

import info.deskchan.core.PluginProxyInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

public class Downloader {
    
    public static void initialize(PluginProxyInterface pluginProxy) {
        pluginProxy.getProperties().load();
        if (pluginProxy.getProperties().getString("default_download_path") == null) {
            pluginProxy.getProperties().put("default_download_path", pluginProxy.getDataDirPath().toString() + "/Downloader");
            pluginProxy.getProperties().save();
        }
        
        pluginProxy.sendMessage("core:add-command", new HashMap<String, Object>() {{
            put("tag", "core:download");
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
            put("msgInfo", new HashMap<String, String>() {{
                put("path", pluginProxy.getString("downloader.set_default_path.path"));
            }});
        }});
        
        pluginProxy.addMessageListener("core:download", (sender, tag, data) -> {
            try {
                Map<String, Object> mapData = (Map) data;
                String urlString = mapData.get("url").toString();
                URL url = new URL(urlString);
                String path = mapData.containsKey("path") ? mapData.get("path").toString() : pluginProxy.getProperties().getString("default_download_path");

                File download_dir = new File(path);
                
                if (!download_dir.exists()) download_dir.mkdir();
                
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
                    pluginProxy.getProperties().put("default_download_path", ((Map) data).get("path").toString());
                    pluginProxy.getProperties().save();
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
