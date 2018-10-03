package info.deskchan.gui_javafx;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import javafx.application.Platform;

import javax.swing.*;
import java.util.ConcurrentModificationException;

public class TrayMenu extends Menu{

    private volatile SystemTray trayRef = null;
    private volatile dorkbox.systemTray.Menu trayMenu = null;
    
    public TrayMenu(){
        super();
        SystemTray systemTray = SystemTray.get();
        if (SystemTray.get() == null) {
            Main.log("Failed to load SystemTray, type dorkbox");
        } else {
            systemTray.setTooltip(App.NAME);
            systemTray.setImage(App.ICON_URL);
            systemTray.setStatus(App.NAME);
            trayRef = systemTray;
            trayMenu = trayRef.getMenu();
        }
    }

    @Override
    public synchronized void update(){
        if(trayMenu != null && (trayMenu instanceof dorkbox.systemTray.ui.swing._SwingTray ||
                trayMenu instanceof dorkbox.systemTray.ui.awt._AwtTray))
            SwingUtilities.invokeLater(this::updateImpl);
        else Platform.runLater(this::updateImpl);
    }

    @Override
    protected synchronized void updateImpl(){

        trayMenu.clear();
        trayRef.setStatus(App.NAME);

        trayMenu.add(new MenuItem(Main.getString("options"), optionsMenuItemAction));
        trayMenu.add(new MenuItem(Main.getString("send-top"), frontMenuItemAction));

        trayMenu.add(new Separator());
        try {
            for (PluginMenuItem it : menuItems) {
                trayMenu.add(toDorkBoxItem(it));
            }
        } catch (ConcurrentModificationException e) {
            Main.log("Concurrent modification by tray. Write us if it cause you lags.");
            return;
        }
        trayMenu.add(new Separator());
        trayMenu.add(new MenuItem(Main.getString("quit"), quitMenuItemAction));

        super.updateImpl();
    }

    protected dorkbox.systemTray.Entry toDorkBoxItem(PluginMenuItem item){

        if (item instanceof PluginAction) return new MenuItem(item.name, ((PluginAction) item).action);
        if (item instanceof PluginSeparator) return new dorkbox.systemTray.Separator();
        if (item instanceof PluginLabel) return new dorkbox.systemTray.MenuItem(item.name);
        if (item instanceof PluginMenu){
            dorkbox.systemTray.Menu dm = new dorkbox.systemTray.Menu(item.name);
            for(PluginMenuItem action : ((PluginMenu) item).actions){
                dm.add(toDorkBoxItem(action));
            }
            return dm;
        }
        return null;
    }
}