package info.deskchan.core_utils;

import info.deskchan.core.PluginManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TerminalGUI {

    private static CoreTimerTask readInTimer = new CoreTimerTask(Main.getPluginProxy(), 500, true) {
        @Override
        public void run() {
            try {
                //System.out.println(System.in.available());
                if (System.in.available() > 0) {
                    Scanner scanner = new Scanner(System.in);
                    String readString;
                    do {
                        readString = scanner.nextLine();
                        Map<String, Object> data = new HashMap<>();
                        data.put("value", readString);
                        proxy.sendMessage("DeskChan:user-said", data);
                    } while(System.in.available()>0 && scanner.hasNextLine());
                }
            } catch (Throwable e){
                System.out.println("Problems while reading console: "+e.getClass().toString());
            }
        }
    };
    public static void initialize(){
        Main.getPluginProxy().sendMessage("core:register-alternative",
                new HashMap<String, Object>() {{
                    put("srcTag", "DeskChan:say");
                    put("dstTag", "core-utils:say");
                    put("priority", 1000);
                }}
        );
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
        });
        System.out.println("started");
        readInTimer.start();
    }
}
