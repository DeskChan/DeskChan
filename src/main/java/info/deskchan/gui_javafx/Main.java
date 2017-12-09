package info.deskchan.gui_javafx;

import info.deskchan.core.*;
import javafx.application.Platform;
import org.apache.commons.lang3.SystemUtils;
import org.jnativehook.GlobalScreen;

import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main implements Plugin {
	
	private static Main instance;
	private PluginProxyInterface pluginProxy;
	private Semaphore appInitSem = new Semaphore(0);

	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		this.pluginProxy = pluginProxy;
		instance = this;

		// Get the logger for "org.jnativehook" and set the level to warning.
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.WARNING);

		// Don't forget to disable the parent handlers.
		logger.setUseParentHandlers(false);

		getProperties().load();

		if(getProperties().containsKey("locale")) {
			Locale.setDefault(new Locale(getProperties().getString("locale")));
			PluginProxy.Companion.updateResourceBundle();
		}

		pluginProxy.setResourceBundle("info/deskchan/gui_javafx/strings");

		new Thread(() -> {
			App.run(PluginManager.getInstance().getArgs());
		}).start();
		try {
			appInitSem.acquire();
		} catch (InterruptedException e) {
			log(e);
		}
		return true;
	}
	
	@Override
	public void unload() {
		synchronized (this) {
			getProperties().save();
		}

		if (SystemUtils.IS_OS_WINDOWS) {
			MouseEventNotificator.disableHooks();
		}
		try {
			Platform.exit();
		} catch (Exception e){
			log(e);
		}
	}
	
	static Main getInstance() {
		return instance;
	}

	PluginProxyInterface getPluginProxy() { return pluginProxy; }
	
	Semaphore getAppInitSem() {
		return appInitSem;
	}
	
	void quit() {
		pluginProxy.sendMessage("core:quit", 0);
	}
	
	static void log(String text) {
		instance.pluginProxy.log(text);
	}
	
	static void log(Throwable e) {
		instance.pluginProxy.log(e);
	}

	public static String getString(String text){
		return getInstance().pluginProxy.getString(text);
	}
	
	static synchronized PluginProperties getProperties() {
		return getInstance().getPluginProxy().getProperties();
	}

}
