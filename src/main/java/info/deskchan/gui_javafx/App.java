package info.deskchan.gui_javafx;

import info.deskchan.core.MessageListener;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.gui_javafx.panes.Balloon;
import info.deskchan.gui_javafx.panes.CharacterBalloon;
import info.deskchan.gui_javafx.panes.UserBalloon;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.stage.*;
import javafx.stage.Window;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class App extends Application {

	static final String NAME = "DeskChan";
	public static final URL ICON_URL = App.class.getResource("icon.png");
	
	private static App instance = null;
	static final List<SkinLoader> skinLoaders = Arrays.asList(
			new SingleImageSkin.Loader(), new ImageSetSkin.Loader(), new DaytimeDependentSkin.Loader()
	);

	private info.deskchan.gui_javafx.panes.Character character;
	private List<DelayNotifier> delayNotifiers = new LinkedList<>();

	static float getTime(long start){
		return (System.currentTimeMillis() - start) / 1000.f;
	}
	/** Initializing plugin. **/
	@Override
	public void start(Stage primaryStage) {
		instance = this;
		long start = System.currentTimeMillis();

		character = new info.deskchan.gui_javafx.panes.Character("main", Skin.load(Main.getProperties().getString("skin.name")));
		Main.log("character initialized, " + getTime(start));
		// Hacking javafx Application class to hide app from programs panel
		HackJavaFX.process();
		Main.log("hacked JavaFX, " + getTime(start));
		// Loading balloon asset
		CharacterBalloon.updateDrawer();
		UserBalloon.updateBalloonSprite();
		Main.log("balloon loaded, " + getTime(start));
		// Trying to apply 'style.css' to application
		Main.log("stylesheets overrided, " + getTime(start));
		// Forbid auto closing program if there is no program windows
		Platform.setImplicitExit(false);
		// Tray and right click menus initialization
		if (Main.getProperties().getBoolean("use-tray", true)){
		    Main.log("initializing menu with tray");
			new TrayMenu();
		} else {
            Main.log("initializing menu without tray");
			new Menu();
		}
		Main.log("tray initialized, " + getTime(start));
		// Loading fonts from 'assets/fonts' folder
		loadFonts();
		Main.log("fonts loaded, " + getTime(start));
		// Transparent window initialization
		OverlayStage.initialize();
		Main.log("overlay initialized, " + getTime(start));
		OverlayStage.updateStage();
		Main.log("overlay setted, " + getTime(start));
		// Registering plugin's API
		initMessageListeners();
		Main.log("message listeners initialized, " + getTime(start));
		// Keyboard initialization
		KeyboardEventNotificator.initialize();
		Main.log("keyboard initialized, " + getTime(start));
		Main.getInstance().getAppInitSem().release();
		Main.log("semaphore released, " + getTime(start));

		// DeskChan saying "Loading"
		character.say(new HashMap<String, Object>() {{
			put("text", Main.getString("info.loading"));
			put("characterImage", "LOADING");
			put("priority", 20000);
			put("timeout", 500000);
		}});
		character.say(new HashMap<String, Object>() {{
			put("text", Main.getString("info.not-loading"));
			put("priority", 19999);
			put("timeout", 500000);
		}});
	}
	
	static App getInstance() {
		return instance;
	}
	
	info.deskchan.gui_javafx.panes.Character getCharacter() {
		return character;
	}

	static void run(String[] args) {
		launch(args);
	}

	/** Show default notification.
	 * @param name Title of window
	 * @param text Text of window **/
	public static void showNotification(String name, String text){
		Platform.runLater(() -> {
			TemplateBox dialog = new TemplateBox("notification", name);
			dialog.setContentText(text);
			dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			dialog.show();
			dialog.requestFocus();
		});
	}

	/** Show default notification.
	 * @param name Title of window
	 * @param content Content inside window **/
	public static void showNotification(String name, Node content){
		Platform.runLater(() -> {
			TemplateBox dialog = new TemplateBox("notification", name);
			dialog.setGraphic(content);
			dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			dialog.setResizable(true);
			dialog.show();
			dialog.requestFocus();
		});
	}

	/** Registering plugin's API. **/
	private void initMessageListeners() {
		PluginProxyInterface pluginProxy = Main.getPluginProxy();

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
				Menu.getInstance().add(sender, (String) m.getOrDefault("name", sender), (String) m.get("msgTag"), m.get("msgData"));
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
				
				Menu.getInstance().add(sender, name, actionList);
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
				character.say(sender, data);
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

		/* Set current style file.
        * Public message
        * Params: path: String! - path to skin
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-interface-style", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Main.getProperties().put("interface.path-skin", (String) data);
				TemplateBox.updateStyle();
			});
		});

		/* Set current skin name of character's balloon .
        * Public message
        * Params: path: String! - path to skin
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-character-balloon-path", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Main.getProperties().put("balloon.path-character", (String) data);
				CharacterBalloon.updateDrawer();
			});
		});

		/* Set current skin name of user's balloon.
        * Public message
        * Params: path: String! - path to skin
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-user-balloon-path", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Main.getProperties().put("balloon.path-user", (String) data);
				UserBalloon.updateBalloonSprite();
			});
		});

		/* Change skin opacity.
        * Public message
        * Params: value:Float! - absolute value, in percents (x100)
        *       or
        *         Map
        *             absolute: Float! - absolute value, float percent
        *           or
        *             relative: Float! - value relative from current, float percent
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-skin-opacity", (sender, tag, data) -> {
			Platform.runLater(() -> {
				System.out.println(data);
				double opacity = 100;
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
					opacity = ((Number) data).floatValue();
				} else {
					opacity = Float.parseFloat(data.toString());
				}

				Main.getProperties().put("skin.opacity", opacity);
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
					character.resizeSkin(((Number) data).floatValue());
				} else {
					character.resizeSkin(Float.parseFloat(data.toString()));
				}

				Main.getProperties().put("skin.scale_factor", character.getScaleFactor());
			});
		});

		/* Resize balloon.
        * Public message
        * Params: value:Float - absolute scaling value, integer percents
        * Returns: None */
		pluginProxy.addMessageListener("gui:resize-balloon", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (data instanceof Map) {
					Map m = (Map) data;
					if (m.containsKey("value"))
						CharacterBalloon.setScaleFactor(((Number) m.get("value")).floatValue());
				} else if (data instanceof Number){
					CharacterBalloon.setScaleFactor(((Number) data).floatValue());
				} else {
					CharacterBalloon.setScaleFactor(Float.parseFloat(data.toString()));
				}
			});
		});

		/* Get character visibility.
		 * Public message
		 * Params: None
		 * Returns: Boolean */
		pluginProxy.addMessageListener("gui:is-character-visible", (sender, tag, data) -> {
			pluginProxy.sendMessage(sender, character.isVisible() && OverlayStage.getInstance().isCharacterVisible());
		});

		/* Set character position.
		 * Public message
		 * Params: Map
		 *          top: Int
		 *          left: Int
		 *          bottom: Int
		 *          right: Int
		 *          verticalPercent: Float
		 *          horizontalPercent: Float
		 * Returns: None */
		pluginProxy.addMessageListener("gui:set-character-position", (sender, tag, data) -> {
			character.relocate((Map<String, Number>) data);
		});

		/* Toggle context menu at right click
        * Public message
        * Params: check: Boolean! - turn menu on/off
        * Returns: None */
		pluginProxy.addMessageListener("gui:toggle-context-menu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Main.getProperties().put("character.enable_context_menu", data);
			});
		});

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:setup-options-tab", (sender, tag, data) -> {
			Platform.runLater(() -> {
				new ControlsPanel(sender, "tab", "tab", (Map) data).set();
			});
		});

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:setup-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				new ControlsPanel(sender, "submenu", "submenu", (Map) data).set();
			});
		});

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:update-options-tab", (sender, tag, data) -> {
			Platform.runLater(() -> {
				new ControlsPanel(sender, "tab", "tab", (Map) data).update();
			});
		});

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:update-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				new ControlsPanel(sender, "submenu", "submenu", (Map) data).update();
			});
		});

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:show-options-dialog", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.open();
			});
		});

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:show-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if(data instanceof Map) {
					new ControlsPanel(sender, "submenu", "submenu", (Map) data).show();
				} else {
					new ControlsPanel(sender, data.toString(), data.toString()).show();
				}
			});
		});

		/* Set panel state.
		 * Public message
		 * Params: Map
		 *           id: String - system id of panel (will be transformed to "sender-id")
		 *           name: String? - title of panel
		 *           type: ControlsPanel.PanelType.toString()? - type of panel, default: tab
		 *           action: String? - set, show, hide, update, delete
		 *           controls: List<Map<String, Object>>
		 *           onSave: String? - if present, adds button 'Save' at the bottom of panel, onSave will be message tag to send panel data
		 *           onClose: String? - if present, onClose will be message tag to send panel data when closed
		 * Returns: None */
		pluginProxy.addMessageListener("gui:set-panel", (sender, tag, data) -> {
			Platform.runLater(() -> {
				new ControlsPanel(sender, (Map) data);
			});
		});

		/* Show panel.
		 * Technical message (use gui:set-panel instead, it presents only as command)
		 * Params: String - id of panel
		 * Returns: None */
		pluginProxy.addMessageListener("gui:show-panel", (sender, tag, data) -> {
			Platform.runLater(() -> {
				ControlsPanel.open(data instanceof Map ? ((Map) data).get("msgData").toString() : data.toString());
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
				if (data instanceof Map) {
					Map<String, Object> m = (Map<String, Object>) data;
					showNotification((String) m.getOrDefault("name", Main.getString("default_messagebox_name")),
							(String) m.get("text"));
				} else {
					showNotification(Main.getString("default_messagebox_name"), data.toString());
				}
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

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:show-custom-window", (sender, tag, data) -> {
			Platform.runLater(() -> {
				new ControlsPanel(sender, "window", "window", (Map) data).show();
			});
		});

		/* DEPRECATED */
		pluginProxy.addMessageListener("gui:update-custom-window", (sender, tag, data) -> {
			Platform.runLater(() -> {
				new ControlsPanel(sender, "window", "window", (Map) data).update();
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
			Platform.runLater(() ->
					Balloon.setDefaultFont((String) data)
			);
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
        * Params: opacity: Integer! - opacity, in percents (x100)
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-balloon-shadow-opacity", (sender, tag, data) -> {
			Platform.runLater(() -> {
				float opacity = ((Number) data).floatValue();
				CharacterBalloon.setShadowOpacity(opacity);
			});
		});

		/* Set skin shadow opacity.
        * Public message
        * Params: opacity: Integer! - opacity, in percents (x100)
        * Returns: None */
		pluginProxy.addMessageListener("gui:set-skin-shadow-opacity", (sender, tag, data) -> {
			Platform.runLater(() -> {
				float opacity = ((Number) data).floatValue();
				Main.getProperties().getFloat("skin.shadow-opacity", opacity);

				if (character != null)
					character.setShadowOpacity(opacity);
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
        *           character-pos: String? - character position, format like "top:50"
		*	        character-size: String? - character size
        *           user-balloon: String? - user balloon file
        *           character-balloon: String? - character balloon file
        *           character-balloon-size: String? - size, 0..1
        *           interface: String? - interface style file
        *           overlay-type: String? - overlay stage type
        * Returns: None */
		pluginProxy.addMessageListener("gui:supply-resource", (sender, tag, data) -> {
			try {
				Map<String, String> map = (Map<String, String>) data;
				if (map.containsKey("skin")) {
					String path = map.get("skin");
					character.setSkin(Skin.load(path));

				} else if (map.containsKey("character-pos")){
				    String[] pos = map.get("character-pos").split(":");
				    character.relocate(pos[0].trim(), Integer.parseInt(pos[1].trim()));

                } else if (map.containsKey("character-size")){
                    String size = map.get("character-size");
                    character.resizeSkin(Float.parseFloat(size));

                } else if (map.containsKey("user-balloon")){
                    String path = map.get("user-balloon");
                    Main.getProperties().put("balloon.path-user", path);
                    UserBalloon.updateBalloonSprite();

                } else if (map.containsKey("character-balloon-size")){
                    String size = map.get("character-balloon-size");
                    CharacterBalloon.setScaleFactor(Float.parseFloat(size));

                } else if (map.containsKey("character-balloon")){
                    String path = map.get("character-balloon");
                    Main.getProperties().put("balloon.path-character", path);
                    CharacterBalloon.updateDrawer();

                } else if (map.containsKey("interface")){
                    String path = map.get("interface");
                    Main.getProperties().put("interface.path-skin", path);
                    TemplateBox.updateStyle();

                } else if (map.containsKey("overlay-type")){
				    String type = map.get("overlay-type");
				    OverlayStage.updateStage(type);

                } else if (map.containsKey("character-balloon-position")){
					String type = map.get("overlay-type");
					OverlayStage.updateStage(type);

				}
			} catch(Exception e){
				Main.log(e);
			}
		});

		/* Toggle recognition redirect to user balloon.
        * Public message
        * Params: check: Boolean! - redirect
        * Returns: None */
		pluginProxy.addMessageListener("gui:toggle-redirect-recognition", (sender, tag, data) -> {
			Main.getProperties().put("redirect-recognition", data);
			pluginProxy.setAlternative("DeskChan:voice-recognition", "gui:raise-user-balloon", (Boolean) data ? 100 : 1);
		});
		pluginProxy.setAlternative("DeskChan:voice-recognition", "gui:raise-user-balloon",
				Main.getProperties().getBoolean("redirect-recognition", false) ? 100 : 1
		);

		/* Open folder containing log file.  */
		pluginProxy.addMessageListener("gui:open-log-file", (sender, tag, data) -> {
			try {
				Desktop.getDesktop().open(Main.getPluginProxy().getDataDirPath().getParent().toFile());
			} catch (Exception e){
				showThrowable(e);
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
				Double value = getDouble(data, 100.0);
				CharacterBalloon.setOpacity(value.floatValue());
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
				String value = (String) m.get("value");
				CharacterBalloon.setDefaultPositionMode(value);
			});
		});

		/* Change balloon direction mode
        * Public message
        * Params: Map
        *           value: String? - direction mode name, "STANDARD_DIRECTION" by default
        * Returns: None */
		pluginProxy.addMessageListener("gui:change-balloon-direction-mode", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				String value = (String) m.get("value");
				Main.getProperties().put("balloon_direction_mode", value);
				CharacterBalloon.setDefaultDirectionMode(value);
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

		/* Raise user balloon. Replaces text inside if already on screen.
        * Public message
        * Params: Map
        *           value: String? - start text
        * Returns: None */
		pluginProxy.addMessageListener("gui:raise-user-balloon", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map m = (Map) data;
				UserBalloon.show(m != null ? (String) m.get("value") : null);
			});
		});

		/* Delete all information about menus and actions of plugin. */
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			Platform.runLater(() -> {
				String pluginId = (String) data;
				for (ControlsPanel panel : ControlsPanel.getPanels(pluginId))
					panel.delete();

				Menu.getInstance().remove(pluginId);
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

		/* Adding notification to reload program at language changing */
		pluginProxy.addMessageListener("core:set-language", (sender, tag, data) -> {
			Platform.runLater(() -> {
				TemplateBox dialog = new TemplateBox("message-box", Main.getString("default_messagebox_name"));
				dialog.setContentText(Main.getString("info.restart"));
				dialog.show();
				dialog.requestFocus();
			});
		});

		MessageListener errorListener = new MessageListener() {
			@Override
			public void handleMessage(String sender, String tag, Object data) {
				Platform.runLater(() -> {
					Map map = (Map) data;
					App.showThrowable(sender, (String) map.get("class"), (String) map.get("message"), (List) map.get("stacktrace"));
				});
			}
		};

		/* Show error alert. */
		pluginProxy.addMessageListener("core-events:error", errorListener);
		pluginProxy.addMessageListener("gui:show-error",    errorListener);

		/* Registering all alternatives. */
		pluginProxy.setAlternative("DeskChan:register-simple-action", "gui:register-simple-action", 100);
		pluginProxy.setAlternative("DeskChan:register-simple-actions", "gui:register-simple-actions", 100);
		pluginProxy.setAlternative("DeskChan:say", "gui:say", 100);
		pluginProxy.setAlternative("core-utils:notify-after-delay", "gui:notify-after-delay", 100);
		pluginProxy.setAlternative("DeskChan:show-technical", "gui:show-notification", 100);

		pluginProxy.sendMessage("core:add-command", new HashMap(){{
			put("tag", "gui:show-character");
			put("info", Main.getString("show-character-info"));
		}});
		pluginProxy.sendMessage("core:set-event-link", new HashMap(){{
			put("eventName", "speech:get");
			put("commandName", "gui:show-character");
			put("rule", Main.getString("show-rule"));
		}});
		pluginProxy.sendMessage("core:add-command", new HashMap(){{
			put("tag", "gui:hide-character");
			put("info", Main.getString("hide-character-info"));
		}});
		pluginProxy.sendMessage("core:set-event-link", new HashMap(){{
			put("eventName", "speech:get");
			put("commandName", "gui:hide-character");
			put("rule", Main.getString("hide-rule"));
		}});
		pluginProxy.sendMessage("core:add-command", new HashMap(){{
			put("tag", "gui:show-options-dialog");
			put("info", Main.getString("show-options-dialog-info"));
		}});
		pluginProxy.sendMessage("core:set-event-link", new HashMap(){{
			put("eventName", "speech:get");
			put("commandName", "gui:show-options-dialog");
			put("rule", Main.getString("show-options-rule"));
		}});
		pluginProxy.sendMessage("core:add-command", new HashMap(){{
			put("tag", "gui:raise-user-balloon");
			put("info", Main.getString("raise-user-balloon-info"));
		}});
		pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>(){{
			put("eventName", "gui:keyboard-handle");
			put("commandName", "gui:raise-user-balloon");
			put("rule", "ALT+Q");
		}});
		pluginProxy.sendMessage("core:add-command", new HashMap(){{
			put("tag", "gui:show-panel");
			put("info", Main.getString("show-panel-info"));
			put("msgInfo", Main.getString("panel-id-info"));
		}});
		pluginProxy.sendMessage("core:add-command", new HashMap(){{
			put("tag", "gui:set-character-position");
			put("info", Main.getString("set-character-position-info"));
			put("msgInfo", new HashMap<String, String>(){{
				put("top", Main.getString("position.top"));
				put("bottom", Main.getString("position.bottom"));
				put("left", Main.getString("position.left"));
				put("right", Main.getString("position.right"));
				put("horizontalPercent", Main.getString("position.horizontalPercent"));
				put("verticalPercent", Main.getString("position.verticalPercent"));
			}});
		}});
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

	/** Load all fonts from 'assets/fonts'. **/
	private void loadFonts() {
		try (DirectoryStream<Path> directoryStream =
					 Files.newDirectoryStream(Main.getPluginProxy().getAssetsDirPath().resolve("fonts"))) {
			for (Path fontPath : directoryStream) {
				if (fontPath.getFileName().toString().endsWith(".ttf")) {
					Font.loadFont(Files.newInputStream(fontPath), 10);
				}
			}
		} catch (IOException e) {
			Main.log(e);
		}
		LocalFont.setDefaultFont(Main.getProperties().getString("interface.font"));
		CharacterBalloon.setDefaultFont(Main.getProperties().getString("balloon.font"));
	}

	/** Trying to apply 'style.css' to application. **/
	public static String getStylesheet(){
		String stylefile = Main.getProperties().getString("interface.path-skin");

		if (stylefile == null || !new File(stylefile).exists())
			stylefile = Main.getPluginProxy().getAssetsDirPath().resolve("styles").resolve("main_blue.css").toString();

		try {
			return new File(stylefile).toURI().toURL().toString();
		} catch (Exception e){
			Main.log(e);
			return App.class.getResource("style.css").toExternalForm();
		}
	}

	/** Get interface size multiplier. **/
	static double getInterfaceScale(){
		return LocalFont.defaultFont.getSize() * Screen.getPrimary().getDpi() / 96;
	}

	/** Show error dialog. **/
	static void showThrowable(Throwable e) {
		showThrowable(Main.getPluginProxy().getId(), e.getClass().toString(), e.getMessage(), Arrays.asList(e.getStackTrace()));
	}

	static void showThrowable(String sender, String className, String message, List<StackTraceElement> stacktrace) {
		if (!Main.getProperties().getBoolean("error-alerting", true)) return;

		Alert alert = new Alert(Alert.AlertType.ERROR);
		((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(App.ICON_URL.toString()));

		alert.setTitle(Main.getString("error"));
		alert.initModality(Modality.WINDOW_MODAL);
		alert.setHeaderText(className + " " + Main.getString("caused-by") + " " + sender);
		alert.setContentText(message);
		StringBuilder exceptionText = new StringBuilder ();
		for (Object item : stacktrace)
			exceptionText.append((item != null ? item.toString() : "null") + "\n");

		TextArea textArea = new TextArea(exceptionText.toString());
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);

		CheckBox checkBox = new CheckBox(Main.getString("enable-error-alert"));
		checkBox.setSelected(Main.getProperties().getBoolean("error-alerting", true));
		checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
			Main.getProperties().put("error-alerting", newValue);
		});

		BorderPane pane = new BorderPane();
		pane.setTop(checkBox);
		pane.setCenter(textArea);

		alert.getDialogPane().setExpandableContent(pane);
		alert.show();
	}

	private static TemplateBox waitingAlert;
	private static boolean needAlert;
	static void showWaitingAlert(Runnable caller) {
		needAlert = true;
		if (waitingAlert == null)
			Main.getPluginProxy().setTimer(800, (sender, data) -> {
				if (!needAlert || waitingAlert != null) return;
				waitingAlert = new TemplateBox("waiting", Main.getString("default_messagebox_name"));
				waitingAlert.getDialogPane().setContent(new Label(Main.getString("wait")));
				waitingAlert.show();
			});

		new Thread(() -> {
			caller.run();
			needAlert = false;
			Platform.runLater(() -> {
				if (waitingAlert != null) {
					waitingAlert.close();
					waitingAlert = null;
				}
			});
		}).start();
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
			Main.getPluginProxy().sendMessage(tag, null);
			stop();
		}
	}

}
