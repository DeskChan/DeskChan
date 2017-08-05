package info.deskchan.core_utils;

import info.deskchan.core.PluginProxyInterface;

import java.util.Map;
import java.util.LinkedList;

public class UserSpeechRequest {
    private static LinkedList<UserSpeechRequest> requests;
    public static void initialize(PluginProxyInterface ppi){
        requests=new LinkedList<>();
        ppi.addMessageListener("DeskChan:request-user-speech", (sender, tag, dat) -> {
            Map<String,Object> data=(Map<String,Object>) dat;
            if(data.containsKey("seq")){
                ppi.sendMessage("core-utils:notify-after-delay",TextOperations.toMap("delay: 200"),
                        (s,d) -> requests.addLast(new UserSpeechRequest(sender, (int) data.get("seq")))
                );
            }

        });
        ppi.addMessageListener("DeskChan:user-said", (sender, tag, dat) -> {
           if(requests.size()==0) return;

           UserSpeechRequest toSend=requests.getFirst();
           requests.removeFirst();
           Map<String,Object> data=(Map<String,Object>) dat;
           data.put("seq",toSend.seq);
           ppi.sendMessage(toSend.sender,data);
        });
    }

    String sender;
    int seq;
    private UserSpeechRequest(String sender,int seq){
        this.sender=sender;
        this.seq=seq;
    }
}
