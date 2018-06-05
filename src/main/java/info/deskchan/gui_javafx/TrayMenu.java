package info.deskchan.gui_javafx;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import javafx.application.Platform;

import javax.swing.*;
import java.util.ConcurrentModificationException;

public class TrayMenu extends Menu{

    private volatile SystemTray trayRef = null;
    
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
        }
    }

    @Override
    public synchronized void update(){
        if(trayRef != null && (trayRef.getMenu() instanceof dorkbox.systemTray.ui.swing._SwingTray ||
           trayRef.getMenu() instanceof dorkbox.systemTray.ui.awt._AwtTray))
            SwingUtilities.invokeLater(this::updateImpl);
        else Platform.runLater(this::updateImpl);
    }

    @Override
    protected synchronized void updateImpl(){

        dorkbox.systemTray.Menu menu = trayRef.getMenu();

        menu.clear();
        trayRef.setStatus(App.NAME);

        menu.add(new MenuItem(Main.getString("options"), optionsMenuItemAction));
        menu.add(new MenuItem(Main.getString("send-top"), frontMenuItemAction));

        menu.add(new Separator());
        try {
            for (PluginMenuItem it : menuItems) {
                menu.add(toDorkBoxItem(it));
            }
        } catch (ConcurrentModificationException e) {
            Main.log("Concurrent modification by tray. Write us if it cause you lags.");
            return;
        }
        menu.add(new Separator());
        menu.add(new MenuItem(Main.getString("quit"), quitMenuItemAction));

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