package info.deskchan.core_utils;

import info.deskchan.core.PluginProxyInterface;

import java.util.HashMap;
import java.util.LinkedList;

public class UserSpeechRequest {
    private static LinkedList<UserSpeechRequest> requests = new LinkedList<>();

    public static void initialize(PluginProxyInterface ppi){

        ppi.sendMessage("core:register-alternative", new HashMap<String,Object>(){{
            put("srcTag", "DeskChan:user-said");
            put("dstTag", "core-utils:answer-speech-request");
            put("priority", 2000);
        }});

        ppi.addMessageListener("DeskChan:request-user-speech", (sender, tag, dat) -> {
            requests.addLast(new UserSpeechRequest(sender));
        });

        ppi.addMessageListener("core-utils:answer-speech-request", (sender, tag, dat) -> {
           if(requests.size() == 0) {
               ppi.sendMessage("DeskChan:user-said#core-utils:answer-speech-request", dat);
               return;
           }

           UserSpeechRequest toSend = requests.getFirst();
           requests.removeFirst();

           ppi.sendMessage(toSend.sender, dat);
        });
    }

    private String sender;
    private UserSpeechRequest(String sender){  this.sender = sender;  }
}
