package info.deskchan.gui_javafx;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Menu {

    protected volatile ArrayList<PluginMenuItem> menuItems = new ArrayList<>();
    protected volatile ContextMenu contextMenu = new ContextMenu();
    protected static volatile Menu instance;

    public Menu(){
        instance = this;

        contextMenu.setId("context-menu");
    }

    public static Menu getInstance(){ return instance; }

    public void add(String sender, String name, String msgTag, Object msgData){
        menuItems.add(new PluginAction(sender, name, msgTag, msgData));
        update();
    }
    public void add(String sender, String name, List<Map<String,Object>> actions){
        menuItems.add(new PluginMenu(sender, name, actions));
        update();
    }
    public void remove(String sender){
        Iterator<PluginMenuItem> it = menuItems.iterator();
        while(it.hasNext()){
            PluginMenuItem item = it.next();
            if(item.sender.equals(sender))
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
class PluginMenuItem{
    final String sender;
    final String name;
    public PluginMenuItem(String name){
        this.sender=null;
        this.name=name;
    }
    public PluginMenuItem(String sender, String name){
        this.sender=sender;
        this.name=name;
    }
    javafx.scene.control.MenuItem fxItem = null;

    javafx.scene.control.MenuItem getJavaFXItem(){
        return fxItem;
    }
}
class PluginAction extends PluginMenuItem {
    final String msgTag;
    final Object msgData;
    final MenuItemAction action;

    PluginAction(String sender, String name, String msgTag, Object msgData) {
        super(sender,name);
        this.msgTag = msgTag;
        this.msgData = msgData;
        action = new MenuItemAction() {
            @Override
            protected void run() {
                Main.getInstance().getPluginProxy().sendMessage(msgTag, msgData);
            }
        };

        fxItem = new javafx.scene.control.MenuItem(name);
        fxItem.setOnAction(action);
    }
}
class PluginSeparator extends PluginMenuItem {
    PluginSeparator() {
        super(null);
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