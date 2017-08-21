package info.deskchan.gui_javafx;

import com.sun.javafx.stage.StageHelper;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import info.deskchan.core.PluginProxyInterface;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.stage.*;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	private SystemTray systemTray = null;
	private ContextMenu contextMenu = new ContextMenu();
	private SortedMap<String, List<PluginActionInfo>> pluginsActions = new TreeMap<>();
	private Character character = new Character("main", Skin.load(Main.getProperty("skin.name", null)));
	private List<DelayNotifier> delayNotifiers = new LinkedList<>();

	class ControlsWindow extends TemplateBox {
		private ControlsContainer controls;
		private String owner;
		public ControlsWindow(String name, String owner, ControlsContainer controls){
			super(name);
			this.controls=controls;
			this.owner=owner;
			getDialogPane().setContent(this.controls.createControlsPane(getDialogPane().getScene().getWindow()));
		}
		public String getOwnerName(){
			return owner;
		}
		public void setControls(ControlsContainer controls) {
			this.controls = null;
			getDialogPane().setContent(null);
			System.gc();
			this.controls = controls;
			getDialogPane().setContent(this.controls.createControlsPane(getDialogPane().getScene().getWindow()));
		}
		public void updateControls(List<Map<String,Object>> data){
			if(data!=null)
				controls.updateControlsPane(data);
		}
	}
	private List<ControlsWindow> customWindowOpened = new LinkedList<>();

	@Override
	public void start(Stage primaryStage) {
		instance = this;
		HackJavaFX.process();
		loadFonts();
		initStylesheetOverride();
		initSystemTray();
		OverlayStage normalStage = new OverlayStage("normal");
		normalStage.show();
		try{
			OverlayStage topStage = new OverlayStage("top");
			topStage.setAlwaysOnTop(true);
			topStage.show();
		} catch(Exception e){
			Main.log("Top Overlay Stage is not working on your system, sorry");
		}
		character.setLayerMode(Character.LayerMode.valueOf(Main.getProperty("character.layer_mode", "ALWAYS_TOP")));
		initMessageListeners();
		// KeyboardEventNotificator.initialize();
		Main.getInstance().getAppInitSem().release();
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

	ContextMenu getContextMenu() {
		return contextMenu;
	}
	
	static void run(String[] args) {
		launch(args);
	}
	
	private void initSystemTray() {
		systemTray = SystemTray.get();
		if (systemTray == null) {
			Main.log("Failed to load SystemTray!");
			return;
		}
		systemTray.setTooltip(NAME);
		systemTray.setImage(ICON_URL);
		systemTray.setStatus(NAME);
		rebuildMenu();
	}
	public void setupCustomWindow(String sender, Map<String,Object> data){
		String name = (String) data.getOrDefault("name", Main.getString("default_messagebox_name"));
		setupCustomWindow(sender, new ControlsContainer(name, (List<Map<String, Object>>) data.get("controls"),
				(String) data.getOrDefault("msgTag", null), (String) data.getOrDefault("onClose", null)));

	}
	public void setupCustomWindow(String sender, ControlsContainer container){
		String name = container.name;
		for(ControlsWindow window : customWindowOpened){
			if(window.getTitle().equals(name) && window.getOwnerName().equals(sender)){
				window.setControls(container);
				return;
			}
		}
		ControlsWindow dialog = new ControlsWindow(name, sender, container);
		customWindowOpened.add(dialog);
		dialog.requestFocus();
		dialog.show();
		dialog.getDialogPane().getChildren().get(0).requestFocus();
		dialog.setOnHiding(event -> {
			customWindowOpened.remove(dialog);
		});
		dialog.setOnCloseRequest(event -> {
			customWindowOpened.remove(dialog);
		});
		dialog.getDialogPane().getScene().getWindow().setOnHiding(event -> {
			customWindowOpened.remove(dialog);
		});
		dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(event -> {
			customWindowOpened.remove(dialog);
		});
	}
	public void updateCustomWindow(String sender, Map<String,Object> data){
		String name = (String) data.getOrDefault("name", Main.getString("default_messagebox_name"));
		List<Map<String,Object>> controls = (List<Map<String,Object>>) data.getOrDefault("controls", null);
		if(controls==null) return;
		for(ControlsWindow window : customWindowOpened){
			if(window.getTitle().equals(name)){
				window.updateControls(controls);
				return;
			}
		}
	}
	public void updateCustomWindow(String sender, ControlsContainer container){
		for(ControlsWindow window : customWindowOpened){
			if(window.getTitle().equals(container.name)){
				window.setControls(container);
				return;
			}
		}
	}
	private void initMessageListeners() {
		PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();
		pluginProxy.addMessageListener("gui:register-simple-action", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				PluginActionInfo pluginActionInfo = new PluginActionInfo((String) m.get("name"),
						(String) m.get("msgTag"), m.get("msgData"));
				List<PluginActionInfo> actions = pluginsActions.getOrDefault(sender, null);
				if (actions == null) {
					actions = new ArrayList<>();
					pluginsActions.put(sender, actions);
				}
				actions.add(pluginActionInfo);
				rebuildMenu();
			});
		});
		pluginProxy.addMessageListener("gui:register-simple-actions", (sender, tag, data) -> {
			Platform.runLater(() -> {
				List<Map<String, Object>> actionList = (List<Map<String, Object>>) data;
				
				List<PluginActionInfo> actions = pluginsActions.getOrDefault(sender, null);
				if (actions == null) {
					actions = new ArrayList<>();
					pluginsActions.put(sender, actions);
				}
				
				for (Map<String, Object> m : actionList) {
					PluginActionInfo pluginActionInfo = new PluginActionInfo((String) m.get("name"),
							(String) m.get("msgTag"), m.get("msgData"));
					actions.add(pluginActionInfo);
				}
				rebuildMenu();
			});
		});
		pluginProxy.addMessageListener("gui:say", (sender, tag, data) -> {
			Platform.runLater(() -> {
				character.say(data);
			});
		});
		pluginProxy.addMessageListener("gui:set-image", (sender, tag, data) -> {
			Platform.runLater(() -> {
				character.setIdleImageName(data.toString());
			});
		});
		pluginProxy.addMessageListener("gui:change-skin", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (data instanceof Path) {
					character.setSkin(Skin.load((Path) data));
				} else {
					character.setSkin(Skin.load(data.toString()));
				}
			});
		});
		pluginProxy.addMessageListener("gui:change-skin-opacity", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				boolean save = (boolean) m.getOrDefault("save", false);
				if (m.containsKey("absolute")) {
					Double opacity = getDouble(m.get("absolute"), 1.);
					character.changeOpacity(opacity.floatValue());
				} else if (m.containsKey("relative")) {
					Double opacityIncrement = getDouble(m.get("relative"), 0.);
					character.changeOpacityRelatively(opacityIncrement.floatValue());
				}

				if (save) {
					Float opacity = character.getSkinOpacity();
					Main.setProperty("skin.opacity", opacity.toString());
				}
			});
		});
		pluginProxy.addMessageListener("gui:set-skin-filter", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (data != null) {
					Map<String, Object> m = (Map<String, Object>) data;
					double red, green, blue, opacity;
					red = getDouble(m, "red", 0.0);
					green = getDouble(m, "green", 0.0);
					blue = getDouble(m, "blue", 0.0);
					opacity = getDouble(m, "opacity", 1.0);
					character.setColorFilter(red, green, blue, opacity);
				} else {
					character.setColorFilter(null);
				}
			});
		});
		pluginProxy.addMessageListener("gui:resize-character", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				boolean save = (boolean) m.getOrDefault("save", false);
				if (m.containsKey("scaleFactor")) {
					Double scaleFactor = getDouble(m.get("scaleFactor"), 1.);
					character.resizeSkin(scaleFactor.floatValue());
				} else if (m.containsKey("zoom")) {
					Double zoom = getDouble(m.get("zoom"), 0.);
					character.resizeSkinRelatively(zoom.floatValue());
				} else if (m.containsKey("width") || m.containsKey("height")) {
					character.resizeSkin((Integer) m.get("width"), (Integer) m.get("height"));
				}

				if (save) {
					Float scaleFactor = character.getScaleFactor();
					Main.setProperty("skin.scale_factor", scaleFactor.toString());
				}
			});
		});
		pluginProxy.addMessageListener("gui:setup-options-tab", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.registerPluginMenu(sender, (Map<String, Object>) data, true);
			});
		});
		pluginProxy.addMessageListener("gui:setup-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.registerPluginMenu(sender, (Map<String, Object>) data,false);
			});
		});
		pluginProxy.addMessageListener("gui:update-options-tab", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.updatePluginMenu(sender, (Map<String, Object>) data, true);
			});
		});
		pluginProxy.addMessageListener("gui:update-options-submenu", (sender, tag, data) -> {
			Platform.runLater(() -> {
				OptionsDialog.updatePluginMenu(sender, (Map<String, Object>) data,false);
			});
		});
		pluginProxy.addMessageListener("gui:show-notification", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				TemplateBox dialog = new TemplateBox((String) m.getOrDefault("name", Main.getString("default_messagebox_name")));
				dialog.setContentText((String) m.get("text"));
				dialog.requestFocus();
				dialog.show();
			});
		});
		pluginProxy.addMessageListener("gui:play-sound", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				String filename=null;
				if(m.containsKey("value")) filename=(String) m.get("value");
				if(m.containsKey("file"))  filename=(String) m.get("file");
				AudioClip clip;
				try {
					clip = new AudioClip(Paths.get(filename).toUri().toString());
				} catch(Exception e){
					Main.log(e);
					return;
				}
				Object volume=m.getOrDefault("volume",new Integer(100));
				if     (volume instanceof Integer) clip.setVolume(((Integer) volume).doubleValue()/100);
				else if(volume instanceof Double) clip.setVolume((Double) volume);
				else if(volume instanceof String) clip.setVolume(Double.valueOf((String)volume));
				//if(m.containsKey("count"))
				///	clip.setCycleCount(2);
				clip.play();
			});
		});
		pluginProxy.addMessageListener("gui:show-custom-window", (sender, tag, data) -> {
			Platform.runLater(() -> {
				setupCustomWindow(sender, (Map<String,Object>) data);
			});
		});
		pluginProxy.addMessageListener("gui:update-custom-window", (sender, tag, data) -> {
			Platform.runLater(() -> {
				updateCustomWindow(sender, (Map<String,Object>) data);
			});
		});
		pluginProxy.addMessageListener("gui:send-character-front", (sender, tag, data) -> {
			Platform.runLater(() -> {
				character.toFront();
				character.getParent().toFront();
			});
		});
		pluginProxy.addMessageListener("gui:hide-character", (sender, tag, data) -> {
			Platform.runLater(() -> {
				App.getInstance().getCharacter().setLayerMode(Character.LayerMode.HIDE);
				character.setLayerMode(Character.LayerMode.HIDE);
			});
		});
		pluginProxy.addMessageListener("gui:show-character", (sender, tag, data) -> {
			Platform.runLater(() -> {
				App.getInstance().getCharacter().setLayerMode(Character.LayerMode.valueOf(Main.getProperty("character.layer_mode", "ALWAYS_TOP")));
			});
		});
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

				Window ownerWindow = OverlayStage.getInstance(character.getCurrentLayerName()).getOwner();
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

				Object seq = m.get("seq");
				Map<String, Object> response = new HashMap<>();
				response.put("seq", seq);
				response.put((multiple) ? "paths" : "path", result);
				pluginProxy.sendMessage(sender, response);
			});
		});
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
					OptionsDialog.updateInstanceTabs();
				}
			} catch(Exception e){
				Main.log(e);
			}
		});
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

				Window ownerWindow = OverlayStage.getInstance(character.getCurrentLayerName()).getOwner();
				File chosenFile = chooser.showDialog(ownerWindow);
				String chosenFilePath = (chosenFile != null) ? chosenFile.toString() : null;

				Object seq = m.get("seq");
				Map<String, Object> response = new HashMap<>();
				response.put("seq", seq);
				response.put("path", chosenFilePath);
				pluginProxy.sendMessage(sender, response);
			});
		});
		pluginProxy.addMessageListener("gui:notify-after-delay", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map m = (Map) data;
				Object seq = m.get("seq");
				Iterator<DelayNotifier> iterator = delayNotifiers.iterator();
				while (iterator.hasNext()) {
					DelayNotifier delayNotifier = iterator.next();
					if (delayNotifier.plugin.equals(sender) && delayNotifier.seq.equals(seq)) {
						iterator.remove();
						delayNotifier.timeline.stop();
					}
				}
				Object delayObj = m.getOrDefault("delay", -1L);
				long delay=1000;
				if(delayObj instanceof Number) delay=((Number) delayObj).longValue();
				else if(delayObj instanceof String) delay=Long.valueOf((String) delayObj);
				if (delay > 0) {
					new DelayNotifier(sender, seq, delay);
				}
			});
		});
		pluginProxy.addMessageListener("gui:change-balloon-timeout", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				Double value = getDouble(m, "value", 200);
				Integer val=value.intValue();
				if((boolean) m.getOrDefault("save", false))
					Main.setProperty("balloon.default_timeout", val.toString());
			});
		});
		pluginProxy.addMessageListener("gui:change-balloon-opacity", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				Double value = getDouble(m, "value", 100) / 100;

				if((boolean) m.getOrDefault("save", false))
					Main.setProperty("balloon.opacity", value.toString());
				if(Balloon.getInstance()!=null)
					Balloon.getInstance().setBalloonOpacity(value.floatValue());
			});
		});
		pluginProxy.addMessageListener("gui:change-layer-mode", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				String value=(String) m.getOrDefault("value","ALWAYS_TOP");
				App.getInstance().getCharacter().setLayerMode(Character.LayerMode.valueOf(value));
				if((boolean) m.getOrDefault("save", false))
					Main.setProperty("character.layer_mode", value);
			});
		});
		pluginProxy.addMessageListener("gui:change-balloon-position-mode", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				String value=(String) m.getOrDefault("value","AUTO");
				App.getInstance().getCharacter().setBalloonPositionMode(Balloon.PositionMode.valueOf(value));
			});
		});
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			Platform.runLater(() -> {
				String pluginId = (String) data;
				OptionsDialog.unregisterPluginTabs(pluginId);
				pluginsActions.remove(pluginId);
				rebuildMenu();
				Iterator<DelayNotifier> iterator = delayNotifiers.iterator();
				while (iterator.hasNext()) {
					DelayNotifier delayNotifier = iterator.next();
					if (delayNotifier.plugin.equals((String) data)) {
						iterator.remove();
						delayNotifier.timeline.stop();
					}
				}
			});
		});
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
	public static Boolean getBoolean(Object value, Boolean defaultValue) {
		if (value == null) return defaultValue;
		if (value instanceof Boolean) return (Boolean) value;
		if (value instanceof String) return value.equals("true");
		return defaultValue;
	}

	private Double getDouble(Map<String, Object> map, String key, double defaultValue) {
		return getDouble(map.getOrDefault(key, defaultValue), defaultValue);
	}

	private void rebuildMenu() {
		Menu mainMenu = systemTray.getMenu();
		if (mainMenu instanceof dorkbox.systemTray.swingUI.SwingUI) {
			if (!SwingUtilities.isEventDispatchThread()) {
				SwingUtilities.invokeLater(this::rebuildMenu);
				return;
			}
		}
		mainMenu.clear();
		systemTray.setStatus(NAME);
		ObservableList<javafx.scene.control.MenuItem> contextMenuItems = contextMenu.getItems();
		contextMenuItems.clear();

		MenuItemAction optionsMenuItemAction = new MenuItemAction() {
			@Override
			protected void run() {
				Platform.runLater(App.getInstance()::showOptionsDialog);
			}
		};
		mainMenu.add(new MenuItem(Main.getString("options"), optionsMenuItemAction));
		javafx.scene.control.MenuItem optionsMenuItem = new javafx.scene.control.MenuItem(Main.getString("options"));
		optionsMenuItem.setOnAction(optionsMenuItemAction);

		mainMenu.add(new Separator());
		contextMenuItems.addAll(optionsMenuItem, new SeparatorMenuItem());

		if (pluginsActions.size() > 0) {
			for (Map.Entry<String, List<PluginActionInfo>> entry : pluginsActions.entrySet()) {
				String pluginId = entry.getKey();
				List<PluginActionInfo> actions = entry.getValue();
				if (actions.size() <= 0) {
					continue;
				}
				if (actions.size() == 1) {
					mainMenu.add(actions.get(0).createMenuItemForTray());
					contextMenuItems.add(actions.get(0).createMenuItemForContextMenu());
				} else {
					Menu pluginTrayMenu = new Menu(pluginId);
					mainMenu.add(pluginTrayMenu);
					javafx.scene.control.Menu pluginContextMenu = new javafx.scene.control.Menu(pluginId);
					pluginContextMenu.setMnemonicParsing(false);
					try{
						contextMenuItems.add(pluginContextMenu);
						for (PluginActionInfo action : actions) {
							pluginTrayMenu.add(action.createMenuItemForTray());
							pluginContextMenu.getItems().add(action.createMenuItemForContextMenu());
						}
					} catch(Exception e){
						Main.log("Work with gui while it was not loaded yet");
					}

				}
			}
			mainMenu.add(new Separator());
			contextMenuItems.add(new SeparatorMenuItem());
		}

		MenuItemAction quitAction = new MenuItemAction() {
			@Override
			protected void run() {
				Main.getInstance().quit();
			}
		};
		mainMenu.add(new MenuItem(Main.getString("quit"), quitAction));
		javafx.scene.control.MenuItem quitContextMenuItem = new javafx.scene.control.MenuItem(Main.getString("quit"));
		quitContextMenuItem.setOnAction(quitAction);
		contextMenuItems.add(quitContextMenuItem);
	}
	
	private void showOptionsDialog() {
		OptionsDialog optionsDialog = OptionsDialog.getInstance();
		if (optionsDialog != null) {
			optionsDialog.getDialogPane().getScene().getWindow().requestFocus();
		} else {
			optionsDialog = new OptionsDialog();
			optionsDialog.show();
		}
	}
	
	private void loadFonts() {
		try (DirectoryStream<Path> directoryStream =
					 Files.newDirectoryStream(Skin.getSkinsPath().getParent().resolve("fonts"))) {
			for (Path fontPath : directoryStream) {
				if (fontPath.getFileName().toString().endsWith(".ttf")) {
					Font.loadFont(Files.newInputStream(fontPath), 10);
					Main.log("Loaded font " + fontPath.getFileName().toString());
				}
			}
		} catch (IOException e) {
			Main.log(e);
		}
		Balloon.setDefaultFont(Font.font(
				Main.getProperty("balloon.font.family", "PT Sans"),
				Double.parseDouble(Main.getProperty("balloon.font.size", "16.0"))
		));
	}
	
	private void initStylesheetOverride() {
		StageHelper.getStages().addListener((ListChangeListener<Stage>) change -> {
			while (change.next()) {
				for (Stage stage : change.getAddedSubList()) {
					stage.getScene().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
				}
			}
		});
	}
	
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

	abstract class MenuItemAction implements ActionListener, EventHandler<javafx.event.ActionEvent> {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			run();
		}

		@Override
		public void handle(javafx.event.ActionEvent event) {
			run();
		}

		protected abstract void run();
	}
	
	class PluginActionInfo extends MenuItemAction {
		
		String name;
		String msgTag;
		Object msgData;
		
		PluginActionInfo(String name, String msgTag, Object msgData) {
			this.name = name;
			this.msgTag = msgTag;
			this.msgData = msgData;
		}

		protected void run() {
			Main.getInstance().getPluginProxy().sendMessage(msgTag, msgData);
		}
		
		MenuItem createMenuItemForTray() {
			return new MenuItem(name, this);
		}

		javafx.scene.control.MenuItem createMenuItemForContextMenu() {
			javafx.scene.control.MenuItem menuItem = new javafx.scene.control.MenuItem(name);
			menuItem.setOnAction(this);
			return menuItem;
		}
	}
	
	class DelayNotifier implements EventHandler<javafx.event.ActionEvent> {
		
		private final Timeline timeline;
		private final String plugin;
		private final Object seq;
		
		DelayNotifier(String plugin, Object seq, long delay) {
			this.plugin = plugin;
			this.seq = seq;
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
			Main.getInstance().getPluginProxy().sendMessage(plugin, new HashMap<String, Object>() {{
				put("seq", seq);
			}});
			stop();
		}
	}
	
}
