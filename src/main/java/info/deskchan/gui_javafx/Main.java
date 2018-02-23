package info.deskchan.gui_javafx;

import info.deskchan.core.*;
import javafx.application.Platform;
import org.apache.commons.lang3.SystemUtils;

import java.util.Locale;
import java.util.concurrent.Semaphore;

public class Main implements Plugin {
	
	private static Main instance;
	private PluginProxyInterface pluginProxy;
	private Semaphore appInitSem = new Semaphore(0);

	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		this.pluginProxy = pluginProxy;
		instance = this;

		getProperties().load();

		if(getProperties().containsKey("locale")) {
			Locale.setDefault(new Locale(getProperties().getString("locale")));
			try {
				PluginProxy.Companion.updateResourceBundle();
			} catch (Exception e){
				log(e);
			}
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
	
	public static Main getInstance() {
		return instance;
	}

	public static PluginProxyInterface getPluginProxy() { return instance.pluginProxy; }
	
	Semaphore getAppInitSem() {
		return appInitSem;
	}

	public void quit() {
		pluginProxy.sendMessage("core:quit", 0);
	}

	public static void log(String text) {
		getPluginProxy().log(text);
	}

	public static void log(Throwable e) {
		getPluginProxy().log(e);
	}

	public static String getString(String text){
		return getPluginProxy().getString(text);
	}

	public static synchronized PluginProperties getProperties() {
		return getPluginProxy().getProperties();
	}

}
