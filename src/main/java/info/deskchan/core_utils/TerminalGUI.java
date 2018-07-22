package info.deskchan.core_utils;

import info.deskchan.core.PluginManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TerminalGUI {

    public static void initialize(){
        Main.getPluginProxy().setAlternative("DeskChan:say", "core-utils:say", 1000);
        Main.getPluginProxy().setAlternative("DeskChan:show-technical", "core-utils:show-technical", 1000);

        Main.getPluginProxy().addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
            String name = data.toString();
            if(name.contains("gui"))
                PluginManager.getInstance().unloadPlugin(name);
        });

        Main.getPluginProxy().addMessageListener("core-utils:say", (sender, tag, data) -> {
            String text = "";
            if(data instanceof Map){
                Map<String,Object> mapData = (Map<String,Object>) data;

                String characterImage = (String) mapData.getOrDefault("characterImage", null);
                if (characterImage != null)
                    System.out.println("*looks "+characterImage+"*");

                if(mapData.containsKey("text"))
                    text=(String) mapData.get("text");
                else if(mapData.containsKey("msgData"))
                    text=(String) mapData.get("msgData");

            } else {
                if(data instanceof String)
                     text=(String) data;
                else text=data.toString();
            }

            System.out.println(text);

            if (Main.getPluginProxy().isAskingAnswer(sender))
                Main.getPluginProxy().sendMessage(sender, null);

            Map m = new HashMap(); m.put("msgData", text);
            Main.getPluginProxy().sendMessage("DeskChan:just-said", m);
        });

        Main.getPluginProxy().addMessageListener("core-utils:show-technical", (sender, tag, data) -> {
            String text = "";
            String header = "MESSAGE";
            if(data instanceof Map){
                Map<String, Object> mapData = (Map) data;

                text = (String) mapData.get("text");
                header = mapData.getOrDefault("name", header).toString();
            } else {
                if(data instanceof String)
                    text = (String) data;
                else text = data.toString();
            }
            int dashes = 19 - header.length();
            if (dashes % 2 == 1) dashes--;
            dashes /= 2;
            for (int i=0; i < dashes; i++) System.out.print("-");
            System.out.print(" " + header + " ");
            for (int i=0; i < dashes; i++) System.out.print("-");
            System.out.println(text);
            for (int i=0, l=header.length() + 2 + dashes*2; i < l; i++) System.out.print("-");
        });

        Main.getPluginProxy().setTimer(500, -1, (s, d) -> {
                try {
                    //System.out.println(System.in.available());
                    if (System.in.available() > 0) {
                        Scanner scanner = new Scanner(System.in);
                        String readString;
                        do {
                            readString = scanner.nextLine();
                            Map<String, Object> data = new HashMap<>();
                            data.put("value", readString);
                            Main.getPluginProxy().sendMessage("DeskChan:user-said", data);
                        } while(System.in.available()>0 && scanner.hasNextLine());
                    }
                } catch (Throwable e){
                    System.out.println("Problems while reading console: "+e.getClass().toString());
                }
        });

        System.out.println("started");
    }
}
