package info.deskchan.gui_javafx;

import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core_utils.TextOperations;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class KeyboardEventNotificator implements NativeKeyListener {

    private static KeyboardEventNotificator instance = new KeyboardEventNotificator();

    public static void initialize() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            return;
        }

        PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();

        // registering event
        pluginProxy.sendMessage("core:add-event", TextOperations.toMap("tag: \"gui:keyboard-handle\""));

        pluginProxy.addMessageListener("core:update-links#gui:keyboard-handle", (sender, tag, data) -> {
            updateCommandsList((List) data);
        });

        setSubmenu();
        pluginProxy.addMessageListener("gui:keyboard-submenu-save", (sender, tag, data) -> {
            Main.setProperty("keyboard.delay_handle", ((Map) data).get("delay").toString());
            handler.updateDefaultDelay();
            setSubmenu();
        });

        pluginProxy.addMessageListener("gui:keyboard-get-keycodes", (sender, tag, data) -> {
            pluginProxy.sendMessage("gui:update-options-submenu", new HashMap<String, Object>(){{
                put("name", pluginProxy.getString("hotkeys"));
                List<HashMap<String, Object>> list = new LinkedList<>();

                String raw = "";
                String keys = "";
                System.out.println(currentPressed);
                if(currentPressed.size() > 0) {
                    for (int keyCode : currentPressed)
                        raw += keyCode + " + ";
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
        });

        GlobalScreen.addNativeKeyListener(instance);
        // testing keywords parsing
        // KeyboardCommand.test();
    }

    private static void setSubmenu(){
        PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();
        pluginProxy.sendMessage("gui:setup-options-submenu", new HashMap<String, Object>(){{
            put("name", pluginProxy.getString("hotkeys"));
            put("msgTag", "gui:keyboard-submenu-save");
            List<HashMap<String, Object>> list = new LinkedList<>();
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
                put("value", Main.getString("Get"));
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

    /** Currently pressed keys as raw code. **/
    private static HashSet<Integer> currentPressed = new HashSet<>();

    /** {@link #currentPressed} was changed recently. **/
    private static boolean setChanged = false;

    /** Instead of immediate comparison of keys pressed with commands we do this by timer to lower CPU load. **/
    private static KeyboardTimerHandler handler = new KeyboardTimerHandler();

    /** Key pressed event (called every tick you press the button, not once). **/
    public void nativeKeyPressed(NativeKeyEvent e) {
        if(currentPressed.add(e.getRawCode()) && commands.length > 0) {
            setChanged = true;
            handler.start();
        }
    }

    /** Key released event. **/
    public void nativeKeyReleased(NativeKeyEvent e) {
        currentPressed.remove(e.getRawCode());
        setChanged = true;
        if(currentPressed.size() == 0)
            handler.stop();
    }

    /** I dunno when this called. **/
    public void nativeKeyTyped(NativeKeyEvent e) {
        System.out.println("If you see this message - you have just found some new secret DeskChan function. " +
                        "Tell developers about it as soon as possible.");
        System.out.println("Key Typed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
    }

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
                for(Integer keyCode : command.keyCodes) {
                    if (!currentPressed.contains(keyCode)) {
                        contains = false;
                        break;
                    }
                }
                if(!contains) continue;

                Main.getInstance().getPluginProxy().sendMessage(command.tag, command.msgData);
            }
        }

        private static final String DEFAULT_DELAY = "50";
        static int getDefaultDelay(){
            try {
                return Integer.parseInt(Main.getProperty("keyboard.delay_handle", DEFAULT_DELAY));
            } catch (Exception e){
                Main.setProperty("keyboard.delay_handle", DEFAULT_DELAY);
                return Integer.parseInt(DEFAULT_DELAY);
            }
        }
    }

    static class KeyboardCommand {
        final String tag;
        final Integer[] keyCodes;
        final Object msgData;

        // Map for keywords. Didn't find proper keywords by myself.
        private static final Map<String, Integer> keyMap = new HashMap<>();
        private static final Map<Integer, String> keyNames = new HashMap<>();
        static {
            try {
                String keycodes = new String(Files.readAllBytes(Paths.get(App.class.getResource("keycodes.json").toURI())));
                JSONObject map = new JSONObject(keycodes);
                for(String entry : map.keySet()){
                    Integer code = Integer.parseInt(entry);
                    JSONArray array = map.getJSONArray(entry);

                    keyNames.put(code, array.getString(0));
                    for(Object name : array.toList()){
                        keyMap.put(name.toString(), code);
                    }
                }
            } catch (Exception e){
                Main.log("Error while parsing keycodes map from file");
                Main.log(e);
            }
        }

        public static String getKeyNames(Set<Integer> keyCodes){
            String result = "";
            if(keyCodes.size() == 0) return result;

            for(Integer keyCode : keyCodes)
                result += keyNames.get(keyCode) + " + ";
            return result.substring(0, result.length()-3);
        }


        /** Testing keywords parsing. **/
        public static void test(){
            String[] keyStrings = {"A", "b", "+", "Space", "ALT", "Left ALT", "right Alt", "@", "2", "NUMPAD 2",
                    "PrtScr", "PAUSE BREAK", "$", "№", "П", "ъ", "ПРОБЕЛ", "АльТ", "капс лок", "Эск", "ф5", "f6", "F7"};
            Integer[] keyCodes  = {65, 66, 187, 32, 164, 164, 165, 50, 50, 98,
                44, 19, 52, 51, 71, 221, 32, 164, 20, 27, 116, 117, 118};

            for(int i = 0; i < keyCodes.length; i++) {
                Integer code = parseKey(keyStrings[i]);
                if(!keyCodes[i].equals(code)){
                    System.out.println(keyStrings[i] + ", " + keyCodes[i] + "!=" + code);
                }
            }
        }

        /** All words that wasn't parsed correctly. **/
        private static Set<String> errors = new HashSet<>();

        /** Parsing keyword to raw key code. **/
        public static Integer parseKey(String key){
            try {
                if(key.length() > 1)
                    return Integer.parseInt(key);
            } catch (Exception e){ }
            StringBuilder sb = new StringBuilder(key.toUpperCase());

            // Ф1-24 -> F1-24
            if(sb.length() > 1 && sb.charAt(0) == 'Ф' && (sb.charAt(1) >= '0' && sb.charAt(1) <= '9'))
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

            Integer code = keyMap.get(sb.toString());
            if(code == null)
                errors.add(key);

            return code;
        }

        public KeyboardCommand(Map<String, Object> map){
            tag = (String) map.get("tag");
            msgData = map.get("msgData");
            String rule = (String) map.getOrDefault("rule", "");
            if (rule == null){
                keyCodes = new Integer[0];
                return;
            }

            String[] keys = rule.toUpperCase().split("\\+");
            HashSet<Integer> keySet = new HashSet<>();
            for(String keyName : keys){
                keyName = keyName.trim();
                if(keyName.length() == 0) continue;

                Integer keyCode = parseKey(keyName);
                if(keyCode != null)
                    keySet.add(keyCode);
            }
            keyCodes = keySet.toArray(new Integer[keySet.size()]);
        }
    }
}