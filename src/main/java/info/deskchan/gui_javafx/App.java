package info.deskchan.gui_javafx;

import com.sun.javafx.stage.StageHelper;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import info.deskchan.core.PluginProxy;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class App extends Application {
	
	static final String NAME = "DeskChan";
	static final URL ICON_URL = App.class.getResource("icon.png");
	
	private static App instance = null;
	static final List<SkinLoader> skinLoaders = Arrays.asList(
			new SingleImageSkin.Loader(), new ImageSetSkin.Loader()
	);
	private SystemTray systemTray = null;
	private SortedMap<String, List<PluginActionInfo>> pluginsActions = new TreeMap<>();
	private Character character = new Character("main", Skin.load(Main.getProperty("skin.name", null)));
	private List<DelayNotifier> delayNotifiers = new LinkedList<>();
	
	@Override
	public void start(Stage primaryStage) {
		instance = this;
		HackJavaFX.process();
		loadFonts();
		initStylesheetOverride();
		initSystemTray();
		OverlayStage normalStage = new OverlayStage("normal");
		normalStage.show();
		OverlayStage topStage = new OverlayStage("top");
		topStage.setAlwaysOnTop(true);
		topStage.show();
		character.setLayerMode(Character.LayerMode.valueOf(Main.getProperty("character.layer_mode", "ALWAYS_TOP")));
		initMessageListeners();
		Main.getInstance().getAppInitSem().release();
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
	
	private void initMessageListeners() {
		PluginProxy pluginProxy = Main.getInstance().getPluginProxy();
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
				character.say((Map<String, Object>) data);
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
		pluginProxy.addMessageListener("gui:setup-options-tab", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				OptionsDialog.registerPluginTab(sender, (String) m.get("name"),
						(List<Map<String, Object>>) m.get("controls"), (String) m.getOrDefault("msgTag", null));
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
		pluginProxy.addMessageListener("gui:show-custom-window", (sender, tag, data) -> {
			Platform.runLater(() -> {
				Map<String, Object> m = (Map<String, Object>) data;
				TemplateBox dialog = new TemplateBox((String) m.getOrDefault("name", Main.getString("default_messagebox_name")));
				dialog.getDialogPane().setContent(new ControlsContainer((String) m.get("name"),
						(List<Map<String, Object>>) m.get("controls"), (String) m.getOrDefault("msgTag", null)).createControlsPane());
				dialog.requestFocus();
				dialog.show();
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
				long delay = (Long) m.get("delay");
				if (delay > 0) {
					new DelayNotifier(sender, seq, delay);
				}
			});
		});
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			Platform.runLater(() -> {
				String pluginId = (String) data;
				OptionsDialog.unregisterPluginTabs(pluginId);
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
		mainMenu.add(new MenuItem(Main.getString("options"), event -> {
			Platform.runLater(this::showOptionsDialog);
		}));
		mainMenu.add(new Separator());
		if (pluginsActions.size() > 0) {
			for (Map.Entry<String, List<PluginActionInfo>> entry : pluginsActions.entrySet()) {
				String pluginId = entry.getKey();
				List<PluginActionInfo> actions = entry.getValue();
				if (actions.size() <= 0) {
					continue;
				}
				if (actions.size() == 1) {
					mainMenu.add(actions.get(0).createMenuItem());
				} else {
					Menu pluginMenu = new Menu(pluginId);
					mainMenu.add(pluginMenu);
					for (PluginActionInfo action : actions) {
						pluginMenu.add(action.createMenuItem());
					}
				}
			}
			mainMenu.add(new Separator());
		}
		mainMenu.add(new MenuItem(Main.getString("quit"), event -> Main.getInstance().quit()));
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
	
	class PluginActionInfo implements ActionListener {
		
		String name;
		String msgTag;
		Object msgData;
		
		PluginActionInfo(String name, String msgTag, Object msgData) {
			this.name = name;
			this.msgTag = msgTag;
			this.msgData = msgData;
		}
		
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			Main.getInstance().getPluginProxy().sendMessage(msgTag, msgData);
		}
		
		MenuItem createMenuItem() {
			return new MenuItem(name, this);
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
