package info.deskchan.notification_manager;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;

import java.text.SimpleDateFormat;
import java.util.*;

public class Main implements Plugin{

	private static PluginProxyInterface pluginProxy;


    /** Single phrase in notification. **/
    private static class NotificationPhrase{
        public final String text;
        public final String sender;
        public final Date date;
        int id;
        private static int idCounter = 0;

        public NotificationPhrase(String text, String sender){
            this.text = text;
            this.sender = sender;
            date = new Date();
            id = idCounter;
            idCounter++;
        }
        public Map<String,Object> getMap(){
            Map<String,Object> map = new HashMap<>();
            String dateString = "(" + new SimpleDateFormat("HH:mm:ss").format(date) + ") ";

            map.put("label", dateString + (sender != null ? "[" + sender + "]: " : "") + text);
            map.put("id", "notification"+id);
            map.put("type", "Button");
            map.put("msgTag", "notification:delete");
            map.put("msgData", id);
            map.put("value", "X");

            return map;
        }
    }


    private LinkedList<NotificationPhrase> history = new LinkedList<>();
    // Max. Wert  - über Menü variabel machen
    private int logLength = 12;
    private String currentQuery = "";

	private static Map<String, Object> EMPTY;

    private List<Map> historyToNotification(){
        List<Map> ret = new ArrayList<>();
        List<NotificationPhrase> list = history.subList(Math.max(history.size() - logLength, 0), history.size());
        if(list.size() == 0){
            ret.add(EMPTY);
            return ret;
        }
        for(NotificationPhrase phrase : list){
            ret.add(phrase.getMap());
        }
        return ret;
    }



	private boolean NotificationManagerIsOpened = false;
	
	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {

		// Logbuch-Eintrag
		pluginProxy.log("Notification Manager loading complete");

        this.pluginProxy = pluginProxy;
        pluginProxy.setConfigField("name", pluginProxy.getString("plugin.name"));

        EMPTY = new HashMap<String, Object>(){{
            put("type", "Label");
            put("value", pluginProxy.getString("no-notifications"));
            put("width", 350);
        }};

        /* Open chat request.
        * Public message
        * Params: None
        * Returns: None */
        pluginProxy.addMessageListener("notification:open", (sender, tag, data) -> {

            //pluginProxy.sendMessage("DeskChan:request-say", "START_DIALOG");
            pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{

                put("id", "notification");
                put("action", "show");

                NotificationManagerIsOpened = true;
             }});

        });

        pluginProxy.sendMessage("core:add-command", new HashMap(){{
            put("tag", "notification:open");
            put("info", "notifications.open-info");
        }});
        pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>(){{
            put("eventName", "gui:keyboard-handle");
            put("commandName", "notification:open");
            put("rule", "ALT+N");
        }});

        /* Chat has been closed through GUI. */
        pluginProxy.addMessageListener("notification:closed", (sender, tag, data) -> {
            NotificationManagerIsOpened = false;
        });

        /* Updated textfield input. */
        pluginProxy.addMessageListener("notification:update-textfield", (sender, tag, data) -> {
            //currentQuery = data.toString();
        });

        /* Someone made request to clear all messages from notification window. We're clearing it. */
        pluginProxy.addMessageListener("notification:clear", (sender, tag, data) -> {
            history.clear();

            setPanel();
        });

        pluginProxy.addMessageListener("notification:delete", (sender, tag, data) -> {
            for (NotificationPhrase not : history) {
                if (not.id == (Integer) data) {
                    history.remove(not);
                    break;
                }
            }

            setPanel();
        });


        /* New notification occured */
        pluginProxy.addMessageListener("DeskChan:notify", (sender, tag, data)->{

        	// An der Stelle bekommen wir Map<String, Object> als data
   			//Map data = (Map) dat;
            String text;
            if(data instanceof Map){
                Map m = (Map) data;
                if(m.containsKey("message"))
                    text = (String) m.get("message");
                else if(m.containsKey("msgData"))
                    text = (String) m.get("msgData");
                else text="";
            } else {
                text = data.toString();
            }

            history.add(new NotificationPhrase(text, sender));

            setPanel();

        });

        /* Registering "Open Notification Manager" button in menu. */
        pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>() {{
            put("eventName", "gui:menu-action");
            put("commandName", "notification:open");
            put("rule", pluginProxy.getString("notifications.open"));
        }});

        setPanel();

		return true;
	}

	void setPanel(){
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
            List<Map> input = new ArrayList<Map>();
            input.addAll(historyToNotification());
            input.add(new HashMap<String, Object>() {{
                put("id", "clear");
                put("type", "Button");
                put("msgTag", "notification:clear");
                put("value", pluginProxy.getString("clear"));
            }});

            put("controls", input);
            put("name", pluginProxy.getString("window-name"));
            put("id", "notification");
            put("type", "window");
            put("onClose","notification:closed");
            put("action", "set");
        }});
    }

}
