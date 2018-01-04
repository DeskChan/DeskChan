package info.deskchan.core_utils;

import info.deskchan.core.PluginProxyInterface;

import java.util.HashMap;
import java.util.LinkedList;

public class UserSpeechRequest {
    private static LinkedList<UserSpeechRequest> requests = new LinkedList<>();

    private static HashMap<String,Object> priorityChanger = new HashMap<String,Object>(){{
        put("srcTag", "DeskChan:user-said");
        put("dstTag", "core-utils:answer-speech-request");
        put("priority", 1000);
    }};

    public static void initialize(PluginProxyInterface ppi){
        ppi.addMessageListener("DeskChan:request-user-speech", (sender, tag, dat) -> {
            ppi.setTimer(200, (s, d) -> {
                requests.addLast(new UserSpeechRequest(sender));
                ppi.sendMessage("core:register-alternative", priorityChanger);
            });
        });

        ppi.addMessageListener("core-utils:answer-speech-request", (sender, tag, dat) -> {
           if(requests.size() == 0) return;

           UserSpeechRequest toSend = requests.getFirst();
           requests.removeFirst();

           ppi.sendMessage(toSend.sender, dat);
           if(requests.size() == 0){
               ppi.sendMessage("core:unregister-alternative", priorityChanger);
           }
        });
    }

    String sender;
    private UserSpeechRequest(String sender){  this.sender = sender;  }
}
