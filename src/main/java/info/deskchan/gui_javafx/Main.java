package info.deskchan.gui_javafx;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginManager;
import info.deskchan.core.PluginProxy;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.Semaphore;

public class Main implements Plugin {
	
	private static Main instance;
	private PluginProxy pluginProxy;
	private Semaphore appInitSem = new Semaphore(0);
	private static final ResourceBundle strings = ResourceBundle.getBundle("info/deskchan/gui_javafx/gui-strings");
	private static final Properties properties = new Properties();
	
	@Override
	public boolean initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		instance = this;
		try {
			properties.load(Files.newInputStream(pluginProxy.getDataDirPath().resolve("config.properties")));
		} catch (IOException e) {
			// Ignore
		}
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
			try {
				properties.store(Files.newOutputStream(pluginProxy.getDataDirPath().resolve("config.properties")),
						"DeskChan JavaFX GUI plugin options");
			} catch (IOException e) {
				log(e);
			}
		}

		if (SystemUtils.IS_OS_WINDOWS) {
			MouseEventNotificator.disableHooks();
		}
	}
	
	static Main getInstance() {
		return instance;
	}
	
	PluginProxy getPluginProxy() {
		return pluginProxy;
	}
	
	Semaphore getAppInitSem() {
		return appInitSem;
	}
	
	void quit() {
		pluginProxy.sendMessage("core:quit", null);
	}
	
	static void log(String text) {
		instance.pluginProxy.log(text);
	}
	
	static void log(Throwable e) {
		instance.pluginProxy.log(e);
	}
	
	static synchronized String getString(String key) {
		try {
			String s = strings.getString(key);
			return new String(s.getBytes("ISO-8859-1"), "UTF-8");
		} catch (Throwable e) {
			return key;
		}
	}
	
	static synchronized String getProperty(String key, String def) {
		return properties.getProperty(key, def);
	}
	
	static synchronized void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
	
}
