package info.deskchan.gui_javafx;

import info.deskchan.core.PluginProxyInterface;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class KeyboardEventNotificator implements NativeKeyListener {

    private static KeyboardEventNotificator instance;
    private static int listenerId = -1;

    public static void initialize() {

        instance = new KeyboardEventNotificator();
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            return;
        }

        PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();

        // registering event
        pluginProxy.sendMessage("core:add-event", new HashMap(){{
            put("tag", "gui:keyboard-handle");
            put("info", Main.getString("keyboard-handle-info"));
            put("ruleInfo", Main.getString("keyboard-handle-rule-info"));
        }});
        pluginProxy.addMessageListener("core:update-links:gui:keyboard-handle", (sender, tag, data) -> {
            updateCommandsList((List) data);
        });

        setSubmenu();
        pluginProxy.addMessageListener("gui:keyboard-submenu-save", (sender, tag, data) -> {
            Main.getProperties().put("keyboard.delay_handle", ((Map) data).get("delay").toString());
            handler.updateDefaultDelay();
            setSubmenu();
        });

        /* Print/stop printing key names in options window
         * Technical message
         * Returns: none */
        pluginProxy.addMessageListener("gui:keyboard-get-keycodes", (sender, tag, data) -> {
            if (listenerId < 0){
                listenerId = pluginProxy.setTimer(30000, (s, d) -> {
                    listenerId = -1;
                    Main.getPluginProxy().sendMessage("gui:update-options-submenu", new HashMap<String, Object>(){{
                        put("name", Main.getString("hotkeys"));
                        List<Map<String, Object>> list = new LinkedList<>();
                        list.add(new HashMap<String, Object>() {{
                            put("id", "get-codes");
                            put("value", Main.getString("hotkeys.watch"));
                        }});
                        put("controls", list);
                    }});
                });
                Main.getPluginProxy().sendMessage("gui:update-options-submenu", new HashMap<String, Object>(){{
                    put("name", Main.getString("hotkeys"));
                    List<Map<String, Object>> list = new LinkedList<>();
                    list.add(new HashMap<String, Object>() {{
                        put("id", "get-codes");
                        put("value", Main.getString("hotkeys.stop-watch"));
                    }});
                    put("controls", list);
                }});
            } else {
                pluginProxy.cancelTimer(listenerId);
                listenerId = -1;
                Main.getPluginProxy().sendMessage("gui:update-options-submenu", new HashMap<String, Object>(){{
                    put("name", Main.getString("hotkeys"));
                    List<Map<String, Object>> list = new LinkedList<>();
                    list.add(new HashMap<String, Object>() {{
                        put("id", "get-codes");
                        put("value", Main.getString("hotkeys.watch"));
                    }});
                    put("controls", list);
                }});
            }
        });

        GlobalScreen.addNativeKeyListener(instance);

        // testing keywords parsing
        //KeyboardCommand.test();
    }

    private static void setSubmenu(){
        PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();
        pluginProxy.sendMessage("gui:setup-options-submenu", new HashMap<String, Object>(){{
            put("name", pluginProxy.getString("hotkeys"));
            put("msgTag", "gui:keyboard-submenu-save");
            List<Map<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "code-info");
                put("type", "Label");
                put("hint", Main.getString("get-keys.info"));
                put("value", Main.getString("get-keys"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "raw-codes");
                put("type", "TextField");
                put("value", "");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "keywords");
                put("type", "TextField");
                put("value", "");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "get-codes");
                put("type", "Button");
                put("msgTag", "gui:keyboard-get-keycodes");
                put("value", Main.getString("hotkeys.watch"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "delay");
                put("type", "Spinner");
                put("min", 0);
                put("max", 2000);
                put("hint", Main.getString("hotkeys.delay_info"));
                put("label", pluginProxy.getString("hotkeys.delay"));
                put("value", KeyboardTimerHandler.getDefaultDelay());
            }});
            put("controls", list);
        }});
    }

    static void updateKeysInSubMenu(){
        Main.getPluginProxy().sendMessage("gui:update-options-submenu", new HashMap<String, Object>(){{
            put("name", Main.getString("hotkeys"));
            List<HashMap<String, Object>> list = new LinkedList<>();

            String raw = "";
            String keys = "";
            if(currentPressed.size() > 0) {
                for (KeyPair keyCode : currentPressed)
                    raw += "R" + keyCode.rawCode + " + ";
                raw = raw.substring(0, raw.length()-3);

                keys = KeyboardCommand.getKeyNames(currentPressed);
            }

            final String Fraw = raw, Fkeys = keys;
            list.add(new HashMap<String, Object>() {{
                put("id", "raw-codes");
                put("type", "TextField");
                put("value", Fraw);
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "keywords");
                put("type", "TextField");
                put("value", Fkeys);
            }});
            put("controls", list);
        }});
    }

    static class KeyPair{
        final int keyCode;
        final int rawCode;
        KeyPair(NativeKeyEvent e){
            keyCode = e.getKeyCode(); rawCode = e.getRawCode();
        }
        @Override
        public boolean equals(Object other){
            if (other instanceof String) return toString().equals(other);
            if (other instanceof Number)
                return rawCode == ((Number) other).intValue();
            try {
                return keyCode == ((KeyPair) other).keyCode;
            } catch (Exception e) {
                return false;
            }
        }
        @Override
        public int hashCode(){
            return keyCode;
        }

        @Override
        public String toString(){
            return Integer.toString(keyCode);
        }
    }

    /** Currently pressed keys as raw code. **/
    private static Set<KeyPair> currentPressed = new HashSet<>();

    /** {@link #currentPressed} was changed recently. **/
    private static boolean setChanged = false;

    /** Instead of immediate comparison of keys pressed with commands we do this by timer to lower CPU load. **/
    private static KeyboardTimerHandler handler = new KeyboardTimerHandler();

    /** Key pressed event (called every tick you press the button, not once). **/
    public void nativeKeyPressed(NativeKeyEvent e) {
        if(currentPressed.add(new KeyPair(e)) && commands != null && commands.length > 0) {
            setChanged = true;
            handler.start();
            if (listenerId >= 0){
                updateKeysInSubMenu();
            }
        }
    }

    /** Key released event. **/
    public void nativeKeyReleased(NativeKeyEvent e) {
        currentPressed.remove(new KeyPair(e));
        setChanged = true;
        if(currentPressed.size() == 0)
            handler.stop();
        if (listenerId >= 0){
            updateKeysInSubMenu();
        }
    }

    /** It does nothing but needs to me implemented. **/
    public void nativeKeyTyped(NativeKeyEvent e) { }

    /** Pre-parsed commands and rules. **/
    static KeyboardCommand[] commands;

    /** Preparing command list for comparison. **/
    static void updateCommandsList(List<Map<String, Object>> commandsInfo){
        try {
            KeyboardCommand[] newCommands = new KeyboardCommand[commandsInfo.size()];

            // we parsing rules there. if some word haven't been parsed, we alert this
            KeyboardCommand.errors.clear();
            for (int i = 0; i < commandsInfo.size(); i++)
                newCommands[i] = new KeyboardCommand(commandsInfo.get(i));

            if(KeyboardCommand.errors.size() > 0){
                App.showNotification(Main.getString("error"),
                                     Main.getString("error.keyword") + KeyboardCommand.errors);
            }

            commands = newCommands;

            // so, if there is no commands, we don't need to handle keyboard events
            if(commands.length == 0)
                handler.timeline.stop();

        } catch (Exception e){
            Main.log("Error while updating links list");
        }
    }


    private static class KeyboardTimerHandler implements EventHandler<ActionEvent> {
        private Timeline timeline;

        KeyboardTimerHandler() {
            updateDefaultDelay();
        }

        void updateDefaultDelay(){
            int frame = getDefaultDelay();
            if(frame == 0)
                timeline = null;
            else {
                timeline = new Timeline(new KeyFrame(Duration.millis(frame), this));
                timeline.setCycleCount(Timeline.INDEFINITE);
            }
        }

        void start(){
            if(timeline == null)
                handle(null);
            else
                timeline.play();
        }

        void stop(){
            if(timeline != null)
                timeline.stop();
        }

        // Current keys pressed and commands comparison
        @Override
        public void handle(javafx.event.ActionEvent actionEvent) {
            if (!setChanged) return;
            setChanged = false;

            int size = currentPressed.size();
            for(KeyboardCommand command : commands){
                if(command.keyCodes.length < size) continue;

                boolean contains = true;
                for(Object keyCode : command.keyCodes) {
                    Iterator<KeyPair> it = currentPressed.iterator();
                    while (it.hasNext()){
                        KeyPair key = it.next();
                        if (key.equals(keyCode)) {
                            it = null;
                            break;
                        }
                    }
                    if (it != null){
                        contains = false;
                        break;
                    }
                }
                if(!contains) continue;

                Main.getPluginProxy().sendMessage(command.tag, command.msgData);
            }
        }

        private static final Integer DEFAULT_DELAY = 50;
        static int getDefaultDelay(){
            try {
                return Main.getProperties().getInteger("keyboard.delay_handle", DEFAULT_DELAY);
            } catch (Exception e){
                Main.getProperties().put("keyboard.delay_handle", DEFAULT_DELAY);
                return DEFAULT_DELAY;
            }
        }
    }

    static class KeyboardCommand {
        final String tag;
        final Object[] keyCodes;
        final Object msgData;

        // Map for keywords. Didn't find proper keywords by myself.
        // various string representation of key -> key code
        private static final Map<String, String> keyMap = new HashMap<>();
        // key code -> key name
        private static final Map<String, String> keyNames = new HashMap<>();
        // contains key codes for duplicating keys like Alt and Ctrl
        private static final List<Integer> duplicates = new ArrayList<>();
        static {
            try {
                String os = "unix";
                if (SystemUtils.IS_OS_WINDOWS) os = "win";
                else if (SystemUtils.IS_OS_LINUX) os = "unix";
                else if (SystemUtils.IS_OS_MAC) os = "mac";

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(App.class.getResourceAsStream("keycodes-" + os + ".json"), "UTF8"));

                String keycodes = "", str;
                while ((str = in.readLine()) != null) keycodes += str;
                in.close();
                JSONObject map = new JSONObject(keycodes);
                for(String entry : map.keySet()){
                    JSONArray array = map.getJSONArray(entry);

                    keyNames.put(entry, array.getString(0));
                    for(Object name : array.toList()){
                        keyMap.put(name.toString(), entry);
                    }
                    if (entry.contains("-"))
                        duplicates.add(Integer.parseInt(entry.substring(0, entry.indexOf('-'))));
                }
            } catch (Exception e){
                Main.log("Error while parsing keycodes map from file");
                Main.log(e);
            }
        }

        static boolean isDuplicate(KeyPair pair){
            return duplicates.contains(pair.keyCode);
        }

        public static String getKeyNames(Set<KeyPair> keyCodes){
            String result = "";
            if(keyCodes.size() == 0) return result;

            for(KeyPair keyCode : keyCodes)
                result += keyNames.get(keyCode.toString()) + " + ";
            return result.substring(0, result.length()-3);
        }


        /** Testing keywords parsing. **/
        public static void test(){
            String[] keyStrings = {"A", "b", "+", "Space", "ALT", "Left ALT", "right Alt", "@", "2", "NUMPAD 2",
                    "PrtScr", "PAUSE BREAK", "$", "№", "П", "ъ", "ПРОБЕЛ", "АльТ", "капс лок", "Эск", "ф5", "f6", "F7", "C"};
            String[] keyCodes  = {"30", "48", "13", "57", "56-1", "56-1", "56-0", "3", "3", "3",
                    "3639-1", "3653", "5", "4", "34", "27", "57", "56-1", "58", "1", "63", "64", "65", "46"};

            for(int i = 0; i < keyCodes.length; i++) {
                if(!keyCodes[i].equals(parseKey(keyStrings[i]))){
                    System.out.println(keyStrings[i] + ", " + parseKey(keyStrings[i]) + "!=" + keyCodes[i]);
                }
            }
        }

        /** All words that wasn't parsed correctly. **/
        private static Set<String> errors = new HashSet<>();

        /** Parsing keyword to raw key code. **/
        public static Object parseKey(String key){
            // note that parse algorithm makes all words upper case and removes spaces and duplicating symbols
            try {
                if (key.length() > 1 && Character.toUpperCase(key.charAt(0)) == 'R' && Character.isDigit(key.charAt(1)))
                    return Integer.parseInt(key.substring(1));
            } catch (Exception e){ }

            StringBuilder sb = new StringBuilder(key.toUpperCase());

            // Ф1-24 -> F1-24
            if(sb.length() > 1 && sb.charAt(0) == 'Ф' && Character.isDigit(sb.charAt(1)))
                sb.setCharAt(0, 'F');

            if (sb.length()>1 && sb.charAt(0) == 'Э')  sb.setCharAt(0, 'Е');

            for(int i=1; i < sb.length(); i++) {
                if (sb.charAt(i) == ' ' || sb.charAt(i) == 'Ь' || sb.charAt(i - 1) == sb.charAt(i)) {
                    sb.deleteCharAt(i);
                    i--;
                } else if (sb.charAt(i) == 'Э') {
                    sb.setCharAt(i, 'Е');
                }
            }

            String code = keyMap.get(sb.toString());
            if(code == null)
                errors.add(key);

            return code;
        }

        public KeyboardCommand(Map<String, Object> map){
            tag = (String) map.get("tag");
            msgData = map.get("msgData");
            String rule = (String) map.getOrDefault("rule", "");
            if (rule == null){
                keyCodes = new Object[0];
                return;
            }

            String[] keys = rule.toUpperCase().split("\\+");
            Set<Object> keySet = new HashSet<>();
            for(String keyName : keys){
                keyName = keyName.trim();
                if(keyName.length() == 0) continue;

                Object keyCode = parseKey(keyName);
                if(keyCode != null)
                    keySet.add(keyCode);
            }
            keyCodes = keySet.toArray(new Object[keySet.size()]);
        }
    }
}