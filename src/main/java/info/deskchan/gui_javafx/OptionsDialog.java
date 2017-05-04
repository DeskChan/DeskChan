package info.deskchan.gui_javafx;

import info.deskchan.core.CoreInfo;
import info.deskchan.core.PluginManager;
import info.deskchan.core.PluginProxy;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.FontSelectorDialog;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

class OptionsDialog extends TemplateBox {
	
	private static OptionsDialog instance = null;
	private TabPane tabPane = new TabPane();
	private Button skinManagerButton = new Button();
	private ListView<PluginListItem> pluginsList = new ListView<>();
	private TreeTableView<AlternativeTreeItem> alternativesTable = new TreeTableView<>();
	private static Map<String, List<ControlsContainer>> pluginsTabs = new HashMap<>();
	
	OptionsDialog() {
		super(Main.getString("deskchan_options"));
		instance = this;
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		initTabs();
		getDialogPane().setContent(tabPane);
		setOnHidden(event -> {
			instance = null;
		});
	}
	
	static OptionsDialog getInstance() {
		return instance;
	}
	
	private void initTabs() {
		PluginProxy pluginProxy = Main.getInstance().getPluginProxy();
		BorderPane appearanceTab = new BorderPane();
		GridPane gridPane = new GridPane();
		gridPane.add(new Label(Main.getString("skin")), 0, 0);
		skinManagerButton.setText(App.getInstance().getCharacter().getSkin().toString());
		skinManagerButton.setOnAction(event -> openSkinManager());
		gridPane.add(skinManagerButton, 1, 0);
		gridPane.add(new Label(Main.getString("scale_factor")), 0, 1);
		Spinner<Double> scaleFactorSpinner = new Spinner<>(0.1, 10.0,
				Float.parseFloat(Main.getProperty("skin.scale_factor", "1.0")), 0.05);
		scaleFactorSpinner.valueProperty().addListener((property, oldValue, value) -> {
			Main.setProperty("skin.scale_factor", value.toString());
			App.getInstance().getCharacter().resizeSprite(value.floatValue());
		});
		gridPane.add(scaleFactorSpinner, 1, 1);
		gridPane.add(new Label(Main.getString("balloon_font")), 0, 2);
		Button balloonFontButton = new Button(
				Balloon.getDefaultFont().getFamily() + ", " + Balloon.getDefaultFont().getSize()
		);
		balloonFontButton.setOnAction(event -> {
			FontSelectorDialog dialog = new FontSelectorDialog(Balloon.getDefaultFont());
			dialog.initOwner(getDialogPane().getScene().getWindow());
			Optional<Font> selectedFontOpt = dialog.showAndWait();
			if (selectedFontOpt.isPresent()) {
				Font selectedFont = selectedFontOpt.get();
				Balloon.setDefaultFont(selectedFont);
				Main.setProperty("balloon.font.family", selectedFont.getFamily());
				Main.setProperty("balloon.font.size", String.valueOf(selectedFont.getSize()));
				balloonFontButton.setText(Balloon.getDefaultFont().getFamily() + ", " + Balloon.getDefaultFont().getSize());
			}
		});
		gridPane.add(balloonFontButton, 1, 2);
		gridPane.add(new Label(Main.getString("character.layer_mode")), 0, 3);
		ComboBox<Character.LayerMode> characterLayerModeComboBox = new ComboBox<>();
		characterLayerModeComboBox.setItems(FXCollections.observableList(Arrays.asList(
				Character.LayerMode.values())));
		characterLayerModeComboBox.getSelectionModel().select(App.getInstance().getCharacter().getLayerMode());
		characterLayerModeComboBox.getSelectionModel().selectedItemProperty().addListener(
				(property, oldValue, value) -> {
					App.getInstance().getCharacter().setLayerMode(value);
					Main.setProperty("character.layer_mode", value.toString());
				}
		);
		gridPane.add(characterLayerModeComboBox, 1, 3);
		gridPane.add(new Label(Main.getString("balloon_default_timeout")), 0, 4);
		Spinner<Integer> balloonDefaultTimeoutSpinner = new Spinner<>(0, 120000,
				Integer.parseInt(Main.getProperty("balloon.default_timeout", "15000")), 1000);
		balloonDefaultTimeoutSpinner.valueProperty().addListener((property, oldValue, value) -> {
			Main.setProperty("balloon.default_timeout", value.toString());
		});
		gridPane.add(balloonDefaultTimeoutSpinner, 1, 4);
		gridPane.add(new Label(Main.getString("balloon_position_mode")), 0, 5);
		ComboBox<Balloon.PositionMode> balloonPositionModeComboBox = new ComboBox<>();
		balloonPositionModeComboBox.setItems(FXCollections.observableList(Arrays.asList(
				Balloon.PositionMode.values())));
		balloonPositionModeComboBox.getSelectionModel().select(
				App.getInstance().getCharacter().getBalloonPositionMode());
		balloonPositionModeComboBox.getSelectionModel().selectedItemProperty().addListener(
				(property, oldValue, value) -> {
					App.getInstance().getCharacter().setBalloonPositionMode(value);
				}
		);
		gridPane.add(balloonPositionModeComboBox, 1, 5);
		//appearanceTab.setTop(gridPane);
		tabPane.getTabs().add(new Tab(Main.getString("appearance"), gridPane));
		BorderPane pluginsTab = new BorderPane();
		pluginsTab.setCenter(pluginsList);
		pluginsList.setPrefSize(400, 300);
		pluginProxy.addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
			Platform.runLater(() -> {
				for (PluginListItem item : pluginsList.getItems()) {
					if (item.id.equals(data)) {
						return;
					}
				}
				pluginsList.getItems().add(new PluginListItem(data.toString(), false));
			});
		});
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			Platform.runLater(() -> {
				pluginsList.getItems().removeIf(item -> item.id.equals(data) && !item.blacklisted);
			});
		});
		for (String id : PluginManager.getInstance().getBlacklistedPlugins()) {
			pluginsList.getItems().add(new PluginListItem(id, true));
		}
		HBox hbox = new HBox();
		Button button = new Button(Main.getString("load"));
		button.setOnAction(event -> {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle(Main.getString("load_plugin"));
			File file = chooser.showDialog(OptionsDialog.this.getDialogPane().getScene().getWindow());
			if (file != null) {
				Path path = file.toPath();
				try {
					PluginManager.getInstance().loadPluginByPath(path);
				} catch (Throwable e) {
					App.showThrowable(OptionsDialog.this.getDialogPane().getScene().getWindow(), e);
				}
			}
		});
		hbox.getChildren().add(button);
		Button unloadPluginButton = new Button(Main.getString("unload"));
		Button blacklistPluginButton = new Button(Main.getString("plugin_list.blacklist"));
		ChangeListener<PluginListItem> pluginListItemChangeListener = (observableValue, oldItem, item) -> {
			unloadPluginButton.setDisable((item == null) || item.blacklisted || item.id.equals("core") ||
					item.id.equals(Main.getInstance().getPluginProxy().getId()));
			blacklistPluginButton.setDisable((item == null) || item.id.equals("core") ||
					item.id.equals(Main.getInstance().getPluginProxy().getId()));
			blacklistPluginButton.setText(((item != null) && item.blacklisted)
					? Main.getString("plugin_list.unblacklist") : Main.getString("plugin_list.blacklist"));
		};
		unloadPluginButton.setOnAction(event -> {
			PluginListItem item = pluginsList.getSelectionModel().getSelectedItem();
			if (item.blacklisted) {
				return;
			}
			if (item.id.equals("core")) {
				return;
			}
			if (item.id.equals(Main.getInstance().getPluginProxy().getId())) {
				return;
			}
			PluginManager.getInstance().unloadPlugin(item.id);
		});
		hbox.getChildren().add(unloadPluginButton);
		blacklistPluginButton.setOnAction(event -> {
			PluginListItem item = pluginsList.getSelectionModel().getSelectedItem();
			if (item.blacklisted) {
				item.blacklisted = false;
				PluginManager.getInstance().removePluginFromBlacklist(item.id);
				PluginManager.getInstance().tryLoadPluginByName(item.id);
			} else {
				item.blacklisted = true;
				PluginManager.getInstance().addPluginToBlacklist(item.id);
			}
			pluginsList.getItems().set(pluginsList.getSelectionModel().getSelectedIndex(), item);
			pluginListItemChangeListener.changed(pluginsList.getSelectionModel().selectedItemProperty(),
					pluginsList.getSelectionModel().getSelectedItem(),
					pluginsList.getSelectionModel().getSelectedItem());
		});
		hbox.getChildren().add(blacklistPluginButton);
		pluginsTab.setBottom(hbox);
		pluginsList.getSelectionModel().selectedItemProperty().addListener(pluginListItemChangeListener);
		tabPane.getTabs().add(new Tab(Main.getString("plugins"), pluginsTab));
		BorderPane alternativesTab = new BorderPane();
		alternativesTab.setCenter(alternativesTable);
		alternativesTable.setPrefSize(400, 300);
		{
			TreeTableColumn<AlternativeTreeItem, String> column = new TreeTableColumn<>(Main.getString("tag"));
			column.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().tag));
			alternativesTable.getColumns().add(column);
			column = new TreeTableColumn<>(Main.getString("plugin"));
			column.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().plugin));
			alternativesTable.getColumns().add(column);
			column = new TreeTableColumn<>(Main.getString("priority"));
			column.setCellValueFactory(param -> {
				int priority = param.getValue().getValue().priority;
				return new ReadOnlyStringWrapper((priority >= 0) ? String.valueOf(priority) : null);
			});
			alternativesTable.getColumns().add(column);
		}
		alternativesTable.setShowRoot(false);
		pluginProxy.sendMessage("core:query-alternatives-map", null, (sender, data) -> {
			Map<String, List<Map<String, Object>>> m1 = (Map<String, List<Map<String, Object>>>)
					((Map<String, Object>) data).get("map");
			final TreeItem<AlternativeTreeItem> root = new TreeItem<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : m1.entrySet()) {
				final TreeItem<AlternativeTreeItem> group = new TreeItem<>(new AlternativeTreeItem(entry.getKey()));
				for (Map<String, Object> m2 : entry.getValue()) {
					final TreeItem<AlternativeTreeItem> item = new TreeItem<>(new AlternativeTreeItem(
							m2.get("tag").toString(),
							m2.get("plugin").toString(),
							(int) m2.get("priority")
					));
					group.getChildren().add(item);
				}
				root.getChildren().add(group);
			}
			alternativesTable.setRoot(root);
		});
		tabPane.getTabs().add(new Tab(Main.getString("alternatives"), alternativesTab));
		BorderPane debugTab = new BorderPane();
		TextField debugMsgTag = new TextField("DeskChan:say");
		debugTab.setTop(debugMsgTag);
		TextArea debugMsgData = new TextArea("{\n\"text\": \"Test\"\n}");
		debugTab.setCenter(debugMsgData);
		button = new Button(Main.getString("send"));
		button.setOnAction(event -> {
			String tag = debugMsgTag.getText();
			String dataStr = debugMsgData.getText();
			try {
				JSONObject json = new JSONObject(dataStr);
				Object data = json.toMap();
				Main.getInstance().getPluginProxy().sendMessage(tag, data);
			} catch (Throwable e) {
				App.showThrowable(OptionsDialog.this.getDialogPane().getScene().getWindow(), e);
			}
		});
		debugTab.setBottom(button);
		tabPane.getTabs().add(new Tab(Main.getString("debug"), debugTab));
		for (Map.Entry<String, List<ControlsContainer>> entry : pluginsTabs.entrySet()) {
			for (ControlsContainer tab : entry.getValue()) {
				tabPane.getTabs().add(new Tab(tab.name, tab.createControlsPane()));
			}
		}
		gridPane = new GridPane();
		gridPane.setHgap(6);
		gridPane.setVgap(6);
		Label label = new Label(CoreInfo.get("NAME") + " " + CoreInfo.get("VERSION"));
		label.setFont(Font.font(20));
		gridPane.add(label, 0, 0, 2, 1);
		gridPane.add(new Label(Main.getString("about.site")), 0, 1);
		Hyperlink hyperlink = new Hyperlink();
		hyperlink.setText(CoreInfo.get("PROJECT_SITE_URL"));
		hyperlink.setOnAction(event -> {
			App.getInstance().getHostServices().showDocument(hyperlink.getText());
		});
		gridPane.add(hyperlink, 1, 1);
		gridPane.add(new Label(Main.getString("about.git_branch")), 0, 2);
		gridPane.add(new Label(CoreInfo.get("GIT_BRANCH_NAME")), 1, 2);
		gridPane.add(new Label(Main.getString("about.git_commit_hash")), 0, 3);
		gridPane.add(new Label(CoreInfo.get("GIT_COMMIT_HASH")), 1, 3);
		gridPane.add(new Label(Main.getString("about.build_datetime")), 0, 4);
		gridPane.add(new Label(CoreInfo.get("BUILD_DATETIME")), 1, 4);
		tabPane.getTabs().add(new Tab(Main.getString("about"), gridPane));
	}
	
	private void openSkinManager() {
		SkinManagerDialog dialog = new SkinManagerDialog(getDialogPane().getScene().getWindow());
		dialog.showAndWait();
		skinManagerButton.setText(App.getInstance().getCharacter().getSkin().toString());
		Main.setProperty("skin.name", App.getInstance().getCharacter().getSkin().getName());
	}
	
	static void registerPluginTab(String plugin, String name, List<Map<String, Object>> controls, String msgTag) {
		List<ControlsContainer> tabs = pluginsTabs.getOrDefault(plugin, null);
		ControlsContainer poTab = new ControlsContainer(name, controls, msgTag);
		if (tabs == null) {
			tabs = new ArrayList<>();
			pluginsTabs.put(plugin, tabs);
			tabs.add(poTab);
			return;
		} else {
			boolean found = false;
			for (int i = 0; i < tabs.size(); i++) {
				if (tabs.get(i).name.equals(name)) {
					tabs.set(i, poTab);
					found = true;
					break;
				}
			}
			if (!found) {
				tabs.add(poTab);
			}
		}
		
		if (instance != null) {
			for (Tab tab : instance.tabPane.getTabs()) {
				if (tab.getText().equals(name)) {
					tab.setContent(poTab.createControlsPane());
					break;
				}
			}
		}
	}
	
	static void unregisterPluginTabs(String plugin) {
		pluginsTabs.remove(plugin);
	}
	
	private static class PluginListItem {
		
		String id;
		boolean blacklisted;
		
		PluginListItem(String id, boolean blacklisted) {
			this.id = id;
			this.blacklisted = blacklisted;
		}
		
		@Override
		public String toString() {
			return blacklisted ? (id + " [BLACKLISTED]") : id;
		}
		
	}
	
	private static class AlternativeTreeItem {
		
		String tag;
		String plugin;
		int priority;
		
		AlternativeTreeItem(String tag, String plugin, int priority) {
			this.tag = tag;
			this.plugin = plugin;
			this.priority = priority;
		}
		
		AlternativeTreeItem(String tag) {
			this(tag, null, -1);
		}
		
	}
}
