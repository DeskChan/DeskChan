package info.deskchan.gui_javafx;

import com.sun.javafx.stage.StageHelper;
import info.deskchan.core.PluginProxyInterface;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.stage.*;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class App extends Application {

	static final String NAME = "DeskChan";
	static final URL ICON_URL = App.class.getResource("icon.png");
	
	private static App instance = null;
	static final List<SkinLoader> skinLoaders = Arrays.asList(
			new SingleImageSkin.Loader(), new ImageSetSkin.Loader(), new DaytimeDependentSkin.Loader()
	);

	private Character character = new Character("main", Skin.load(Main.getProperties().getString("skin.name")));
	private List<DelayNotifier> delayNotifiers = new LinkedList<>();

	/** Initializing plugin. **/
	@Override
	public void start(Stage primaryStage) {
		instance = this;
		// Hacking javafx Application class to hide app from programs panel
		HackJavaFX.process();
		// Loading fonts from 'assets/fonts' folder
		loadFonts();
		// Trying to apply 'style.css' to application
		initStylesheetOverride();
		// Forbid auto closing program if there is no program windows
		Platform.setImplicitExit(false);
		// Tray and right click menus initialization
		TrayMenu.initialize();
		// Transparent window initialization
		OverlayStage.initialize();
		OverlayStage.updateStage();
		// Registering plugin's API
		initMessageListeners();
		// Registering keyboard handlers
		KeyboardEventNotificator.initialize();

		Main.getInstance().getAppInitSem().release();

		// DeskChan saying "Loading"
		character.say(new HashMap<String,Object>(){{
			put("text", Main.getString("info.loading"));
			put("priority",20000);
			put("timeout",500000);
		}});
		character.say(new HashMap<String,Object>(){{
			put("text",Main.getString("info.not-loading"));
			put("priority",19999);
			put("timeout",500000);
		}});
	}
	
	static App getInstance() {
		return instance;
	}
	
	Character getCharacter() {
		return character;
	}

	static void run(String[] args) {
		launch(args);
	}

	/** Show default notification.
	 * @param name Title of window
	 * @param text Text of window **/
	public static void showNotification(String name, String text){
		TemplateBox dialog = new TemplateBox(name);
		dialog.setContentText(text);
		dialog.requestFocus();
		dialog.show();
	}

	/** Registering plugin's API. **/
	private void initMessageListeners() {
		PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();

		/* Registering single action that will be visible in click menu.
        * Public message
        * Params: Map
        *           name: String? - text of menu item
        *           msgTag: String! - tag to call when menu item will be chosen
        *           msgData: String? - data to send when menu item will be chosen
        * Returns: None */
		pluginProxy.addMessageListener("gui:register-simple-action", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map m = (Map) data;
				if(!m.containsKey("msgTag")){
					Main.log("Not enough data to setup simple action, received by "+sender);
					return;
				}
				TrayMenu.add(sender, (String) m.getOrDefault("name", sender), (String) m.get("msgTag"), m.get("msgData"));
			});
		});

		/* Registering submenu with single actions that will be visible in click menu.
        * Public message
        * Params: Map
        *           name:String - submenu text
        *           List of Maps
        *             name: String? - text of menu item
        *             msgTag: String! - tag to call when menu item will be chosen
        *             msgData: String? - data to send when menu item will be chosen
        * Returns: None */
		pluginProxy.addMessageListener("gui:register-simple-actions", (sender, tag, data) -> {
			Platform.runLater(() -> {
				List<Map<String, Object>> actionList;
				String name = sender;
				if(data instanceof List){
					actionList = (List<Map<String, Object>>) data;
				} else if(data instanceof Map){
					name = ((Map) data).getOrDefault("name", name).toString();
					actionList = (List) ((Map) data).get("actions");
				} else {
					Main.log("Cannot convert "+data.getClass().toString()+" to actions list, send by "+sender);
					return;
				}
				
				TrayMenu.add(sender, name, actionList);
			});
		});

		/* Request to say something on behalf of DeskChan
        * Public message
        * Params: text: String! - message text
        *     or
        *         Map
        *           text: String! - message text
		*		    skippable: Boolean? - message can be skipped, true by defalut
		*		    characterImage: String? - sprite name to show with image, do not change current by default
		*		    timeout: Integer? - time that message will be shown
		*		    priority: Integer? - priority of message
		*		    partible: Boolean? - message can be divided to parts
        * Returns: None */
		pluginProxy.addMessageListener("gui:say", (sender, tag, data) -> {
			Platform.runLater(() -> {
				character.say(data);
			});
		});

		/* Set current sprite name showing on screen. Message will not be skipped.
        * Public message
        * Params: name: String? - name of sprite, "normal" by default
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-image", (sender, tag, data) -> {
			Platform.runLater(() -> {
				character.setImageName(data != null ? data.toString() : null);
			});
		});

		/* Set current skin name showing on screen. Message will not be skipped.
        * Public message
        * Params: path: String! - path to skin
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-skin", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (data instanceof Path) {
					character.setSkin(Skin.load((Path) data));
				} else {
					character.setSkin(Skin.load(data.toString()));
				}
			});
		});

		/* Change skin opacity.
        * Public message
        * Params: value:Float! - absolute value, integer percent
        *       or
        *         Map
        *             absolute: Float! - absolute value, float percent
        *           or
        *             relative: Float! - value relative from current, float percent
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-skin-opacity", (sender, tag, data) -> {
			Platform.runLater(() -> {
				double opacity = 1;
				if (data instanceof Map) {
					Map<String, Object> m = (Map<String, Object>) data;
					if (m.containsKey("absolute")) {
						opacity = getDouble(m.get("absolute"), 1.);
					} else if (m.containsKey("relative")) {
						Double opacityIncrement = getDouble(m.get("relative"), 0.);
						character.changeOpacityRelatively(opacityIncrement.floatValue());
						return;
					}
				} else if (data instanceof Number){
					opacity = ((Number) data).floatValue() / 100;
				} else {
					opacity = Float.parseFloat(data.toString()) / 100;
				}

				Main.getProperties().put("balloon.opacity", opacity);
				character.changeOpacity((float) opacity);
			});
		});

		/* Set skin filter.
        * Public message
        * Params: Map
        *           red: Float! - red component
        *           green: Float! - red component
        *           blue: Float! - red component
        *           opacity: Float! - opacity of filter
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-skin-filter", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (data != null) {
					Map<String, Object> m = (Map<String, Object>) data;
					double red, green, blue, opacity;
					red =     getDouble(m, "red",     0.0);
					green =   getDouble(m, "green",   0.0);
					blue =    getDouble(m, "blue",    0.0);
					opacity = getDouble(m, "opacity", 1.0);
					character.setColorFilter(red, green, blue, opacity);
				} else {
					character.setColorFilter(null);
				}
			});
		});

		/* Resize character.
        * Public message
        * Params: value:Float - absolute scaling value, integer percents
        *       or
        *         Map
        *             scaleFactor: Double! - absolute scaling value, float percents
        *           or
        *             zoom: Float! - scaling value relative from current, float percents
        *           or
        *             width: Integer! - width of image
        *             height: Integer! - height of image
        * Returns: None */
		pluginProxy.addMessageListener("gui:resize-character", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (data instanceof Map) {
					Map<String, Object> m = (Map<String, Object>) data;
					if (m.containsKey("scaleFactor")) {
						Double scaleFactor = getDouble(m.get("scaleFactor"), 1.);
						character.resizeSkin(scaleFactor.floatValue());
					} else if (m.containsKey("zoom")) {
						Double zoom = getDouble(m.get("zoom"), 0.);
						character.resizeSkinRelatively(zoom.floatValue());
					} else if (m.containsKey("width") || m.containsKey("height")) {
						character.resizeSkin((Integer) m.get("width"), (Integer) m.get("height"));
					}
				} else if (data instanceof Number){
					character.resizeSkin(((Number) data).floatValue() / 100);
				} else {
					character.resizeSkin(Float.parseFloat(data.toString()) / 100);
				}

				Main.getProperties().put("skin.scale_factor", character.getScaleFactor());
			});
		});

		/* Set up options container as tab.
        * Public message
        * Params: Map
        *           name: String! - text of tag
        *           msgTag: String? - tag to send controls values when 'Save' button clicked
        *           onClose: String? - tag to call when menu closed
        *           controls: List! of Maps - look at documentation
        * Returns: None */
		pluginProxy.addMessageListener("gui:setup-options-tab", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.registerPluginMenu(sender, (Map) data, true);
			});
		});

		/* Set up options container as submenu in 'Plugins' tab.
        * Public message
        * Params: look at gui:setup-options-tab
        * Returns: None */
		pluginProxy.addMessageListener("gui:setup-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.registerPluginMenu(sender, (Map) data, false);
			});
		});

		/* Update values in options tab.
        * Public message
        * Params: look at gui:setup-options-tab
        * Returns: None */
		pluginProxy.addMessageListener("gui:update-options-tab", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.updatePluginMenu(sender, (Map) data, true);
			});
		});

		/* Update values in options submenu.
        * Public message
        * Params: look at gui:setup-options-tab
        * Returns: None */
		pluginProxy.addMessageListener("gui:update-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.updatePluginMenu(sender, (Map) data, false);
			});
		});

		/* Show options dialog.
        * Public message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("gui:show-options-dialog", (sender, tag, data) -> {
			Platform.runLater(this::showOptionsDialog);
		});

		/* Show options submenu.
        * Public message
        * Params: Map
        *           owner: String? - owner of menu
        *           menu: String? - name of menu
        * Returns: None */
		pluginProxy.addMessageListener("gui:show-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if(data instanceof Map) {
					Map map = (Map) data;
					OptionsDialog.openSubMenu((String) map.get("owner"),
											  (String) map.get("menu"));
				} else {
					OptionsDialog.openSubMenu(sender, data.toString());
				}
			});
		});

		/* Show notification.
        * Public message
        * Params: Map
        *           name: String? - title of notification
        *           text: String? - text of notification
        * Returns: None */
		pluginProxy.addMessageListener("gui:show-notification", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				showNotification( (String) m.getOrDefault("name", Main.getString("default_messagebox_name")),
						(String) m.get("text"));
			});
		});

		/* Play sound.
        * Public message
        * Params: Map
        *           file: String! - path to sound
        *           volume: Integer? - volume
        * Returns: None */
		pluginProxy.addMessageListener("gui:play-sound", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				String filename = null;
				if(m.containsKey("value")) filename = (String) m.get("value");
				if(m.containsKey("file"))  filename = (String) m.get("file");
				if(filename == null || filename.equals("")){
					Main.log("Received empty file to play");
					return;
				}
				AudioClip clip;
				try {
					clip = new AudioClip(Paths.get(filename).toUri().toString());
				} catch(Exception e){
					Main.log(e);
					return;
				}
				Object volume = m.getOrDefault("volume", 100);
				if     (volume instanceof Number) clip.setVolume(((Number) volume).doubleValue() / 100);
				else if(volume instanceof String) clip.setVolume(Double.valueOf((String)volume));
				//if(m.containsKey("count"))
				///	clip.setCycleCount(2);
				clip.play();
			});
		});

		/* Show custom window.
        * Public message
        * Params: look at gui:setup-options-tab
        * Returns: None */
		pluginProxy.addMessageListener("gui:show-custom-window", (sender, tag, data) -> {
			Platform.runLater(() -> {
				ControlsWindow.setupCustomWindow(sender, (Map<String,Object>) data);
			});
		});

		/* Update values in custom window.
        * Public message
        * Params: look at gui:setup-options-tab
        * Returns: None */
		pluginProxy.addMessageListener("gui:update-custom-window", (sender, tag, data) -> {
			Platform.runLater(() -> {
				ControlsWindow.updateCustomWindow(sender, (Map<String,Object>) data);
			});
		});

		/* Send character to front.
        * Public message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("gui:send-character-front", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OverlayStage.getInstance().toFront();
			});
		});

		/* Hide character.
        * Public message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("gui:hide-character", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OverlayStage.updateStage("HIDE");
			});
		});

		/* Show character.
        * Public message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("gui:show-character", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OverlayStage.updateStage();
			});
		});

		/* Set balloon font.
        * Public message
        * Params: font: String! - font in inner format
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-balloon-font", (sender, tag, data) -> {
			Balloon.setDefaultFont((String) data);
		});

		/* Set interface size.
        * Public message
        * Params: size: Float! - size multiplier
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-interface-size", (sender, tag, data) -> {
			Main.getProperties().put("interface-size", data);
		});

		/* Set interface font.
        * Public message
        * Params: font: String! - font in inner format
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-interface-font", (sender, tag, data) -> {
			LocalFont.setDefaultFont((String) data);
		});

		/* Set balloon shadow opacity.
        * Public message
        * Params: opacity: Integer! - opacity, 0-100
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-balloon-shadow-opacity", (sender, tag, data) -> {
			float opacity = ((Number) data).floatValue() / 100;
			Main.getProperties().getFloat("skin.shadow-opacity", opacity);

			if (Balloon.getInstance() != null)
				Balloon.getInstance().setShadowOpacity(opacity);
		});

		/* Set skin shadow opacity.
        * Public message
        * Params: opacity: Integer! - opacity, 0-100
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-skin-shadow-opacity", (sender, tag, data) -> {
			float opacity = ((Number) data).floatValue() / 100;
			Main.getProperties().getFloat("balloon.shadow-opacity", opacity);

			if (character != null)
				character.setShadowOpacity(opacity);
		});

		/* Open skin dialog.
        * Public message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("gui:open-skin-dialog", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.getInstance().openSkinManager();
			});
		});

		/* Open file choosing dialog.
        * Public message
        * Params: Map
        *           title: String? - dialog title
        *           filters: List of Maps - file filters
        *           description: String? - dialog description
        *           initialDirectory: String? - dialog initial directory
        *           initialFilename: String? - dialog initial filename
        *           multiple: Boolean? - multiple files can be selected, false as default
        *           saveDialog: Boolean? - dialog for file saving, false as default
        * Returns: String - selected filename
        *        or
        *          List of String - selected filenames list */
		pluginProxy.addMessageListener("gui:choose-files", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				FileChooser chooser = new FileChooser();
				chooser.setTitle((String) m.getOrDefault("title", Main.getString("chooser.file.default_title")));

				List<Map<String, Object>> filters = (List<Map<String, Object>>) m.getOrDefault("filters", null);
				for (Map<String, Object> filter : filters) {
					String description = (String) filter.getOrDefault("description", null);
					List<String> extensions = (List<String>) filter.getOrDefault("extensions", null);
					if (description != null && extensions != null) {
						chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
					}
				}

				String initialDirectory = (String) m.getOrDefault("initialDirectory", null);
				if (initialDirectory != null) {
					Path initialDirectoryPath = Paths.get(initialDirectory);
					if (Files.isDirectory(initialDirectoryPath)) {
						chooser.setInitialDirectory(initialDirectoryPath.toFile());
					}
				}

				String initialFilename = (String) m.getOrDefault("initialFilename", null);
				if (initialFilename != null) {
					chooser.setInitialFileName(initialFilename);
				}

				Window ownerWindow = OverlayStage.getInstance().getOwner();
				Object result;
				boolean multiple = (boolean) m.getOrDefault("multiple", false);
				if (multiple) {
					List<File> chosenFiles = chooser.showOpenMultipleDialog(ownerWindow);
					if (chosenFiles != null) {
						result = chosenFiles.stream().map(File::toString).collect(Collectors.toList());
					} else {
						result = null;
					}
				} else {
					boolean saveDialog = (boolean) m.getOrDefault("saveDialog", false);
					File chosenFile = (saveDialog) ? chooser.showSaveDialog(ownerWindow) : chooser.showOpenDialog(ownerWindow);
					result = (chosenFile != null) ? chosenFile.toString() : null;
				}

				pluginProxy.sendMessage(sender, result);
			});
		});

		/* Supply resources handler
        * Public message
        * Params: Map
        *           skin: String? - skin name
        * Returns: None */
		pluginProxy.addMessageListener("gui:supply-resource", (sender, tag, data) -> {
			try {
				Map<String, Object> map = (Map<String, Object>) data;
				if (map.containsKey("skin")) {
					String type=(String)map.get("skin");
					if(!type.startsWith(Skin.getSkinsPath().toString())){
						Path resFile = Paths.get(type);
						Path newPath = Skin.getSkinsPath().resolve(resFile.getFileName());
						try {
							FileUtils.copyDirectory(resFile.toFile(), newPath.toFile());
						} catch (Exception e) {
							e.printStackTrace();
						}
						type = newPath.toString();
					}
					character.setSkin(Skin.load(type));
				}
			} catch(Exception e){
				Main.log(e);
			}
		});

		/* Selecting directories dialog.
        * Public message
        * Params: Map
        *           title: String? - dialog title
        *           initialDirectory: String? - dialog initial directory
        * Returns: Map
        *           path: String - selected directory  */
		pluginProxy.addMessageListener("gui:choose-directory", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle((String) m.getOrDefault("title", Main.getString("chooser.directory.default_title")));

				String initialDirectory = (String) m.getOrDefault("initialDirectory", null);
				if (initialDirectory != null) {
					Path initialDirectoryPath = Paths.get(initialDirectory);
					if (Files.isDirectory(initialDirectoryPath)) {
						chooser.setInitialDirectory(initialDirectoryPath.toFile());
					}
				}

				Window ownerWindow = OverlayStage.getInstance().getOwner();
				File chosenFile = chooser.showDialog(ownerWindow);
				String chosenFilePath = (chosenFile != null) ? chosenFile.toString() : null;

				Map<String, Object> response = new HashMap<>();
				response.put("path", chosenFilePath);
				pluginProxy.sendMessage(sender, response);
			});
		});

		/* Notify after delay.
        * Public message
        * Params: Map
        *             delay: Long?
        *           or
        *             cancel: Integer! - timer identificator
        *           initialDirectory: String? - dialog initial directory
        * Returns: message with None */
		pluginProxy.addMessageListener("gui:notify-after-delay", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map m = (Map) data;

				// canceling timer
				Integer seq = (Integer) m.get("cancel");
				if (seq != null) {
					String cancelTag = sender + "#" + seq;
					Iterator<DelayNotifier> iterator = delayNotifiers.iterator();
					while (iterator.hasNext()) {
						DelayNotifier delayNotifier = iterator.next();
						if (delayNotifier.tag.equals(cancelTag)) {
							iterator.remove();
							delayNotifier.timeline.stop();
						}
					}
					return;
				}

				Object delayObj = m.getOrDefault("delay", -1L);
				long delay = 1000;
				if(delayObj instanceof Number)
					delay = ((Number) delayObj).longValue();
				else
					delay = Long.valueOf(delayObj.toString());

				if (delay > 0) {
					new DelayNotifier(sender, delay);
				}
			});
		});

		/* Change multiplier for balloon timeout, in ms/symbol
        * Public message
        * Params: value: Integer? - multiplier
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-balloon-timeout", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Integer value = getDouble(data, 200.0).intValue();
				Main.getProperties().put("balloon.default_timeout", value);
			});
		});

		/* Change balloon opacity
        * Public message
        * Params: value: Integer? - opacity, 0-100
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-balloon-opacity", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Double value = getDouble(data, 100.0) / 100;
				Main.getProperties().put("balloon.opacity", value.toString());

				if(Balloon.getInstance() != null)
					Balloon.getInstance().setBalloonOpacity(value.floatValue());
			});
		});

		/* Change layer mode
        * Public message
        * Params: Map
        *           value: String? - layer mode name, "ALWAYS_TOP" by default
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-layer-mode", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				String value = (String) m.get("value");
				OverlayStage.updateStage(value);
			});
		});

		/* Change balloon position mode
        * Public message
        * Params: Map
        *           value: String? - position mode name, "AUTO" by default
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-balloon-position-mode", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				String value = (String) m.getOrDefault("value", "AUTO");
				App.getInstance().getCharacter().setBalloonPositionMode(Balloon.PositionMode.valueOf(value));
			});
		});

		/* Switch text animation in balloon
        * Public message
        * Params: check: Boolean! - turn animation on/off
        *       or
        *         delay: Integer! - animation delay in ms, 0 to turn off
        * Returns: None */
		pluginProxy.addMessageListener("gui:switch-balloon-animation", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (data instanceof Boolean){
					Main.getProperties().put("balloon.text-animation-delay", (Boolean) data ? 50 : 0);
				} else if (data instanceof Number){
					Main.getProperties().put("balloon.text-animation-delay", ((Number) data).intValue());
				} else if (data instanceof String){
					try {
						Main.getProperties().put("balloon.text-animation-delay", ((Float) Float.parseFloat(data.toString())).intValue());
					} catch (Exception e){
						Main.getProperties().put("balloon.text-animation-delay", data.toString().equals("true"));
					}
				}
			});
		});

		/* Delete all information about menus and actions of plugin. */
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			Platform.runLater(() -> {
				String pluginId = (String) data;
				OptionsDialog.unregisterPluginMenus(pluginId);
				TrayMenu.remove(pluginId);
				Iterator<DelayNotifier> iterator = delayNotifiers.iterator();
				while (iterator.hasNext()) {
					DelayNotifier delayNotifier = iterator.next();
					if (delayNotifier.tag.startsWith((String) data)) {
						iterator.remove();
						delayNotifier.timeline.stop();
					}
				}
			});
		});

		/* Registering all alternatives. */
		pluginProxy.sendMessage("core:register-alternatives", Arrays.asList(
				new HashMap<String, Object>() {{
					put("srcTag", "DeskChan:register-simple-action");
					put("dstTag", "gui:register-simple-action");
					put("priority", 100);
				}},
				new HashMap<String, Object>() {{
					put("srcTag", "DeskChan:register-simple-actions");
					put("dstTag", "gui:register-simple-actions");
					put("priority", 100);
				}},
				new HashMap<String, Object>() {{
					put("srcTag", "DeskChan:say");
					put("dstTag", "gui:say");
					put("priority", 100);
				}},
				new HashMap<String, Object>() {{
					put("srcTag", "core-utils:notify-after-delay");
					put("dstTag", "gui:notify-after-delay");
					put("priority", 100);
				}}
		));
	}

	/** Parsing value to double or returning default if we failed. **/
	public static Double getDouble(Object value, Double defaultValue) {
		if (value == null) return defaultValue;
		if (value instanceof Number) return ((Number) value).doubleValue();
		if (value instanceof String){
			Double d;
			try {
				d = Double.valueOf((String) value);
			} catch(Exception e){ return defaultValue; }
			return d;
		}
		return defaultValue;
	}

	/** Parsing value to boolean or returning default if we failed. **/
	public static Boolean getBoolean(Object value, Boolean defaultValue) {
		if (value == null) return defaultValue;
		if (value instanceof Boolean) return (Boolean) value;
		if (value instanceof String) return value.equals("true");
		return defaultValue;
	}

	/** Parsing value to double or returning default if we failed. **/
	private Double getDouble(Map<String, Object> map, String key, double defaultValue) {
		return getDouble(map.getOrDefault(key, defaultValue), defaultValue);
	}

	/** Show options dialog. **/
	protected void showOptionsDialog() {
		OptionsDialog optionsDialog = OptionsDialog.getInstance();
		if (optionsDialog != null) {
			optionsDialog.getDialogPane().getScene().getWindow().requestFocus();
		} else {
			optionsDialog = new OptionsDialog();
			optionsDialog.show();
		}
	}

	/** Load all fonts from 'assets/fonts'. **/
	private void loadFonts() {
		try (DirectoryStream<Path> directoryStream =
					 Files.newDirectoryStream(Main.getInstance().getPluginProxy().getAssetsDirPath().resolve("fonts"))) {
			for (Path fontPath : directoryStream) {
				if (fontPath.getFileName().toString().endsWith(".ttf")) {
					Font.loadFont(Files.newInputStream(fontPath), 10);
				}
			}
		} catch (IOException e) {
			Main.log(e);
		}
		Balloon.setDefaultFont(Main.getProperties().getString("balloon.font"));
		LocalFont.setDefaultFont(Main.getProperties().getString("interface.font"));
	}

	/** Trying to apply 'style.css' to application. **/
	private void initStylesheetOverride() {
		try {
			StageHelper.getStages().addListener((ListChangeListener<Stage>) change -> {
				while (change.next()) {
					for (Stage stage : change.getAddedSubList()) {
						stage.getScene().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
					}
				}
			});
		} catch (Throwable e){
			Main.log("Stylesheets deprecated in this version of Java, we didn't fix it yet.");
		}
	}

	/** Get interface size multiplier. **/
	static double getInterfaceMultiplierSize(){
		return Main.getProperties().getDouble("interface-size", Screen.getPrimary().getDpi() / 96);
	}

	/** Show error dialog. **/
	static void showThrowable(Window parent, Throwable e) {
		Main.log(e);
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Exception");
		alert.initOwner(parent);
		alert.initModality(Modality.WINDOW_MODAL);
		alert.setHeaderText(e.getClass().getName());
		alert.setContentText(e.getMessage());
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		String exceptionText = stringWriter.toString();
		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		alert.getDialogPane().setExpandableContent(textArea);
		alert.showAndWait();
	}

	class DelayNotifier implements EventHandler<javafx.event.ActionEvent> {
		
		private final Timeline timeline;
		private final String tag;

		DelayNotifier(String tag, long delay) {
			this.tag = tag;
			timeline = new Timeline(new KeyFrame(Duration.millis(delay), this));
			delayNotifiers.add(this);
			timeline.play();
		}
		
		void stop() {
			timeline.stop();
			delayNotifiers.remove(this);
		}
		
		@Override
		public void handle(javafx.event.ActionEvent actionEvent) {
			Main.getInstance().getPluginProxy().sendMessage(tag, null);
			stop();
		}
	}

}
