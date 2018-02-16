package info.deskchan.gui_javafx;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.*;

public class TrayMenu {

    private static volatile SystemTray trayRef = null;
    private static volatile ArrayList<PluginMenuItem> menuItems = new ArrayList<>();
    private static volatile ContextMenu contextMenu = new ContextMenu();
    
    public static void initialize(){
        SystemTray systemTray = SystemTray.get();
        if (SystemTray.get() == null) {
            Main.log("Failed to load SystemTray, type dorkbox");
        } else {
            systemTray.setTooltip(App.NAME);
            systemTray.setImage(App.ICON_URL);
            systemTray.setStatus(App.NAME);
            trayRef = systemTray;
        }
    }
    public static void add(String sender, String name, String msgTag, Object msgData){
        menuItems.add(new PluginAction(sender, name, msgTag, msgData));
        update();
    }
    public static void add(String sender, String name, List<Map<String,Object>> actions){
        menuItems.add(new PluginMenu(sender, name, actions));
        update();
    }
    public static void remove(String sender){
        Iterator<PluginMenuItem> it = menuItems.iterator();
        while(it.hasNext()){
            PluginMenuItem item = it.next();
            if(item.sender.equals(sender))
                it.remove();
        }
        update();
    }
    
    public static ContextMenu getContextMenu(){
        return contextMenu;
    }
    private static MenuItemAction optionsMenuItemAction = new MenuItemAction() {
        @Override
        protected void run() {
            long start = System.currentTimeMillis();
            Main.log("clicked to open");
            Platform.runLater( () -> {
                Main.log("in thread, "+App.getTime(start));
                OptionsDialog.open();
                Main.log("opened, "+App.getTime(start));
            });  }
    };
    private static MenuItemAction frontMenuItemAction = new MenuItemAction() {
        @Override
        protected void run() {  Platform.runLater(OverlayStage.getInstance()::toFront);   }
    };
    private static MenuItemAction quitMenuItemAction = new MenuItemAction() {
        @Override
        protected void run() {  Platform.runLater(Main.getInstance()::quit);              }
    };
    public synchronized static void update(){
        if(trayRef == null) return;

        if(trayRef.getMenu() instanceof dorkbox.systemTray.ui.swing._SwingTray ||
           trayRef.getMenu() instanceof dorkbox.systemTray.ui.awt._AwtTray)
            SwingUtilities.invokeLater(TrayMenu::updateImpl);
        else Platform.runLater(TrayMenu::updateImpl);
    }
    private synchronized static void updateImpl(){
        dorkbox.systemTray.Menu menu = trayRef.getMenu();

        menu.clear();
        trayRef.setStatus(App.NAME);

        menu.add(new MenuItem(Main.getString("options"),optionsMenuItemAction));
        menu.add(new MenuItem(Main.getString("send-top"),frontMenuItemAction));

        menu.add(new Separator());
        try {
            for (PluginMenuItem it : menuItems) {
                menu.add(it.getDorkBoxItem());
            }
        } catch (ConcurrentModificationException e){
            Main.log("Concurrent modification by tray. Write us if it cause you lags.");
            return;
        }
        menu.add(new Separator());

        menu.add(new MenuItem(Main.getString("quit"), quitMenuItemAction));

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

    dorkbox.systemTray.Entry getDorkBoxItem(){
        return null;
    }
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
    @Override
    dorkbox.systemTray.Entry getDorkBoxItem() {
        return new MenuItem(name, action);
    }
}
class PluginSeparator extends PluginMenuItem {
    PluginSeparator() {
        super(null);
        fxItem = new SeparatorMenuItem();
    }
    @Override
    dorkbox.systemTray.Entry getDorkBoxItem() {
        return new dorkbox.systemTray.Separator();
    }
}
class PluginLabel extends PluginMenuItem {
    PluginLabel(String name) {
        super(name);
        fxItem = new javafx.scene.control.MenuItem(name);
    }
    @Override
    dorkbox.systemTray.Entry getDorkBoxItem() {
        return new dorkbox.systemTray.MenuItem(name);
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
    @Override
    dorkbox.systemTray.Entry getDorkBoxItem() {
        dorkbox.systemTray.Menu dm = new dorkbox.systemTray.Menu(name);
        for(PluginMenuItem action : actions){
            dm.add(action.getDorkBoxItem());
        }
        return dm;
    }
}