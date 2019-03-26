package info.deskchan.gui_javafx;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;

import java.awt.event.ActionListener;
import java.util.*;

public class Menu {

    protected volatile ArrayList<PluginMenuItem> menuItems = new ArrayList<>();
    protected volatile ContextMenu contextMenu = new ContextMenu();
    protected static volatile Menu instance;

    public Menu(){
        instance = this;

        contextMenu.setId("context-menu");

        Main.getPluginProxy().sendMessage("core:add-event", new HashMap<String, Object>(){{
            put("tag", "gui:menu-action");
            put("info", Main.getString("menu-action-info"));
            put("ruleInfo", Main.getString("menu-action-rule-info"));
        }});

        Main.getPluginProxy().addMessageListener("core-events:update-links:gui:menu-action", (sender, tag, data) -> {
            updateCommandsList((List) data);
        });
    }

    void updateCommandsList(List<Map<String, Object>> commandsInfo){
        try {
            // we parsing rules there. if some word haven't been parsed, we alert this
            Iterator<PluginMenuItem> it = menuItems.iterator();
            while(it.hasNext()){
                PluginMenuItem item = it.next();
                if(item instanceof PluginAction && ((PluginAction) item).isCommand)
                    it.remove();
            }

            for (int i = 0; i < commandsInfo.size(); i++) {
                PluginAction item = new PluginAction(commandsInfo.get(i));
                item.isCommand = true;
                menuItems.add(item);
            }
        } catch (Exception e){
            Main.log(new Exception("Error while registering menu actions", e));
        }
        update();
    }

    public static Menu getInstance(){ return instance; }

    public void add(String sender, String name, String msgTag, Object msgData){
        PluginAction action = new PluginAction(sender, name, msgTag, msgData);
        menuItems.remove(action);
        menuItems.add(action);
        update();
    }
    public void add(String sender, String name, List<Map<String,Object>> actions){
        PluginMenu action = new PluginMenu(sender, name, actions);
        menuItems.remove(action);
        menuItems.add(action);
        update();
    }
    public void remove(String sender){
        Iterator<PluginMenuItem> it = menuItems.iterator();
        while(it.hasNext()){
            PluginMenuItem item = it.next();
            if(item.sender != null && item.sender.equals(sender))
                it.remove();
        }
        update();
    }
    public void remove(String sender, String name){
        Iterator<PluginMenuItem> it = menuItems.iterator();
        while(it.hasNext()){
            PluginMenuItem item = it.next();
            if(item.sender.equals(sender) && item.name.equals(name))
                it.remove();
        }
        update();
    }

    public ContextMenu getContextMenu(){
        return contextMenu;
    }

    protected MenuItemAction optionsMenuItemAction = new MenuItemAction() {
        @Override protected void run() {
            Platform.runLater(OptionsDialog::open);
        }
    };
    protected MenuItemAction frontMenuItemAction = new MenuItemAction() {
        @Override
        protected void run() {  Platform.runLater(OverlayStage::sendToFront);   }
    };
    protected MenuItemAction quitMenuItemAction = new MenuItemAction() {
        @Override
        protected void run() {  Platform.runLater(Main.getInstance()::quit);              }
    };

    public synchronized void update(){
        Platform.runLater(this::updateImpl);
    }
    protected synchronized void updateImpl(){

        ObservableList<javafx.scene.control.MenuItem> contextMenuItems = contextMenu.getItems();
        contextMenuItems.clear();

        javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(Main.getString("options"));
        item.setOnAction(optionsMenuItemAction);
        contextMenuItems.add(item);

        item = new javafx.scene.control.MenuItem(Main.getString("send-top"));
        item.setOnAction(frontMenuItemAction);
        contextMenuItems.add(item);

        contextMenuItems.add(new SeparatorMenuItem());
        for(PluginMenuItem it : menuItems){
            contextMenuItems.add(it.getJavaFXItem());
        }
        contextMenuItems.add(new SeparatorMenuItem());

        item = new javafx.scene.control.MenuItem(Main.getString("quit"));
        item.setOnAction(quitMenuItemAction);
        contextMenuItems.add(item);
    }

    abstract class MenuItemAction implements ActionListener, EventHandler<ActionEvent> {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            run();
        }

        @Override
        public void handle(javafx.event.ActionEvent event) {
            run();
        }

        protected abstract void run();
    }

    class PluginMenuItem {
        public final String sender;
        public String name;

        public PluginMenuItem(String name){ this(Main.getPluginProxy().getId(), name); }
        public PluginMenuItem(String sender, String name){
            if (sender == null || name == null) throw new RuntimeException("Menu action cannot have null values: sender=" + sender + ", name=" + name);
            this.sender = sender;
            this.name = name;
        }

        javafx.scene.control.MenuItem fxItem = null;

        javafx.scene.control.MenuItem getJavaFXItem(){
            return fxItem;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PluginMenuItem)) return false;
            PluginMenuItem object = (PluginMenuItem) obj;
            return sender.equals(object.sender) && name.equals(object.name);
        }
    }

    class PluginAction extends PluginMenuItem {
        final String msgTag;
        final Object msgData;
        final MenuItemAction action;
        boolean isCommand = false;

        PluginAction(String sender, String name, String msgTag, Object msgData) {
            super(sender, name);
            if (this.name == null)
                this.name = msgTag;
            this.msgTag = msgTag;
            this.msgData = msgData;
            action = new MenuItemAction() {
                @Override
                protected void run() {
                    Main.getPluginProxy().sendMessage(msgTag, msgData);
                }
            };

            fxItem = new javafx.scene.control.MenuItem(name);
            fxItem.setOnAction(action);
        }

        public PluginAction(Map<String, Object> data){
            this(Main.getPluginProxy().getId(), (String) data.get("rule"), (String) data.get("commandName"), data.get("msgData"));
        }
    }
    class PluginSeparator extends PluginMenuItem {
        PluginSeparator() {
            super("Separator");
            fxItem = new SeparatorMenuItem();
        }
    }
    class PluginLabel extends PluginMenuItem {
        PluginLabel(String name) {
            super(name);
            fxItem = new javafx.scene.control.MenuItem(name);
        }
    }
    class PluginMenu extends PluginMenuItem {
        ArrayList<PluginMenuItem> actions = new ArrayList<>();
        PluginMenu(String sender, String name, List<Map<String,Object>> list) {
            super(sender, name);
            javafx.scene.control.Menu fm = new javafx.scene.control.Menu(name);
            try {
                for(Map<String,Object> action : list){
                    PluginMenuItem item = null;
                    if(action.containsKey("name")){
                        if(action.containsKey("msgTag")){
                            actions.add(item = new PluginAction(sender, (String) action.get("name"), (String) action.get("msgTag"), action.get("msgData")));
                        } else {
                            actions.add(item = new PluginLabel(name));
                        }
                    } else {
                        actions.add(item = new PluginSeparator());
                    }
                    fm.getItems().add(item.getJavaFXItem());
                }
            } catch(Exception e){
                Main.log("Cannot create actions menu, wrong data recieved by "+sender);
                return;
            }
            fxItem = fm;
        }
    }
}

