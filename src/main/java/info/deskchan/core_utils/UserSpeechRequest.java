package info.deskchan.core_utils;

import info.deskchan.core.PluginProxyInterface;

import java.util.LinkedList;
import java.util.List;

public class UserSpeechRequest {
    private static LinkedList<UserSpeechRequest> requests = new LinkedList<>();

    public static void initialize(PluginProxyInterface ppi){

        ppi.setAlternative("DeskChan:user-said", "core-utils:answer-speech-request", 2000);
        ppi.setAlternative("DeskChan:commands-list", "core:commands-list-in-request", 1000);

        ppi.addMessageListener("DeskChan:request-user-speech", (sender, tag, data) -> {
            requests.addLast(new UserSpeechRequest(sender, data));
        });

        ppi.addMessageListener("DeskChan:discard-user-speech", (sender, tag, data) -> {
            ppi.callNextAlternative(sender, "DeskChan:user-said", "core-utils:answer-speech-request", data);
        });


        ppi.addMessageListener("core-utils:answer-speech-request", (sender, tag, data) -> {
           if(requests.size() == 0) {
               ppi.callNextAlternative(sender, "DeskChan:user-said", "core-utils:answer-speech-request", data);
               return;
           }

           UserSpeechRequest toSend = requests.getFirst();
           requests.removeFirst();

           ppi.sendMessage(toSend.sender, data);
        });

        ppi.addMessageListener("core:commands-list-in-request", (sender, tag, data) -> {
            if(requests.size() == 0) {
                ppi.callNextAlternative(sender, "DeskChan:commands-list", "core:commands-list-in-request", data);
                return;
            }
            UserSpeechRequest toSend = requests.getFirst();
            if (toSend.commandsList == null){
                ppi.sendMessage("DeskChan:request-say", "NO_PHRASE");
                return;
            }
            StringBuilder sb;

            if (toSend.commandsList == null){
                sb = new StringBuilder();
            }
            else if (toSend.commandsList instanceof List) {
                sb = new StringBuilder();
                for (String command : (List<String>) toSend.commandsList)
                    sb.append(command + "\n");
            } else {
                sb = new StringBuilder(toSend.commandsList.toString());
            }

            ppi.setTimer(200, (s, d) -> {
                ppi.sendMessage("DeskChan:show-technical", sb.toString());
            });
        });
    }

    private String sender;
    private Object commandsList;
    private UserSpeechRequest(String sender, Object data){
        this.sender = sender;
        commandsList = data;
    }
}
