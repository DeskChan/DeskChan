package info.deskchan.gui_javafx;

import com.sun.javafx.application.PlatformImpl;
import info.deskchan.core.Plugin;
import info.deskchan.core.PluginManager;
import info.deskchan.core.PluginProperties;
import info.deskchan.core.PluginProxyInterface;
import javafx.application.Platform;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.concurrent.Semaphore;

// This file contains only initialization of plugin itself
// Help:

// App.java                      - All message listeners and alternatives initialization
// HackJavaFX.java               - Hacking JavaFX not to show icon of application in OS TaskBar
// KeyboardEventNotificator.java - Keyboard handling and initialization of DeskChan keyboard events module
// LocalFont.java                - Stores default font of DeskChan interface and cast functions Font<->String
// OverlayStage.java             - Initialization of Stage overlaying OS interface
// TemplateBox.java              - Standard Dialog wrapping with custom icon and CSS auto applying
// TrayMenu.java                 - Tray menu (and right-click-menu) implementation

// PluginOptionsControlItem.java - Wrapping of GUI elements to support markup
// ControlsPanel.java            - Standard panel with options elements
// ControlsWindow.java           - Window with controls panel as content
// OptionsDialog.java            - Initialization of Options dialog
// FilesManagerDialog.java       - Dialog that contains list of files with adding/removing functionality

// Skin.java                     - Interface for skin
// SkinLoader.java               - Interface for skin loader
// ImageSetSkin.java             - Skin containing set of images with autoreplacing when set contains not enough emotions
// DaytimeDependentSkin.java     - Skin that adds filter with shading/lightning at certain time of day
// SingleImageSkin.java          - Skin containing single image

// MouseEventNotificator.java    - Native mouse handling and notifications of mouse events

public class Main implements Plugin {
	
	private static Main instance;
	private PluginProxyInterface pluginProxy;
	private Semaphore appInitSem = new Semaphore(0);

	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		this.pluginProxy = pluginProxy;
		instance = this;

		getProperties().load();

		pluginProxy.setResourceBundle("info/deskchan/gui_javafx/strings");
		pluginProxy.setConfigField("name", pluginProxy.getString("plugin-name"));
		pluginProxy.getProperties().putIfHasNot("use-tray", true);
		pluginProxy.getProperties().putIfHasNot("skin.name", "illia");
		pluginProxy.getProperties().putIfHasNot("sprites-animation-delay", 20);
        pluginProxy.getProperties().putIfHasNot("balloon.text-animation-delay", 50);

		new Thread(() -> {
			try {
				App.run(PluginManager.getInstance().getArgs());
			} catch (NoClassDefFoundError e){
				pluginProxy.log("WARNING! JavaFX is not installed. GUI plugin is now closing. Please, install JavaFX.");
				appInitSem.release();
				instance = null;
			}
		}).start();
		try {
			appInitSem.acquire();
		} catch (InterruptedException e) {
			log(e);
		}

		return instance != null;
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
		pluginProxy.sendMessage("core:quit", null);
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

	public static void runLater(Runnable runnable) {
		PlatformImpl.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					runnable.run();
				} catch (Throwable e){
					getPluginProxy().log(e);
				}
			}
		});
	}

}
