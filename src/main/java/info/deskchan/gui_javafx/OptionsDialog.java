package info.deskchan.gui_javafx;

import info.deskchan.core.CommandsProxy;
import info.deskchan.core.CoreInfo;
import info.deskchan.core.PluginManager;
import info.deskchan.core.PluginProxyInterface;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.converter.DefaultStringConverter;
import org.controlsfx.dialog.FontSelectorDialog;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

class OptionsDialog extends TemplateBox {

	private static OptionsDialog instance = null;
	private TabPane tabPane = new TabPane();
	private Button skinManagerButton = new Button();
	private Button balloonFontButton = new Button();
	private ListView<PluginListItem> pluginsList = new ListView<>();
	private TreeTableView<AlternativeTreeItem> alternativesTable = new TreeTableView<>();
	private TableView<CommandItem> commandsTable=new TableView<>();
	private static Map<String, List<ControlsContainer>> pluginsTabs = new HashMap<>();

	OptionsDialog() {
		super(Main.getString("deskchan_options"));
		instance = this;
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

	static void updateInstanceTabs(){
		getInstance().skinManagerButton.setText(App.getInstance().getCharacter().getSkin().toString().replaceAll(
				String.format(".*\\%c", File.separatorChar), ""));
		getInstance().balloonFontButton.setText(Balloon.getDefaultFont().getFamily() + ", " + Balloon.getDefaultFont().getSize());
	}

	private void initMainTab(){
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
		list.add(new HashMap<String, Object>() {{
			put("id", "skin");
			put("type", "Button");
			put("label", Main.getString("skin"));
			put("hint",Main.getString("help.skin"));
			put("value", App.getInstance().getCharacter().getSkin().toString().replaceAll(
				String.format(".*\\%c", File.separatorChar), ""));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id", "scale");
			put("type", "Spinner");
			put("label", Main.getString("skin.scale_factor"));
			put("min", 10);
			put("max", 1000);
			put("step", 5);
			put("msgTag","gui:change-character-scale");
			double scaleFactorValue = Float.parseFloat(Main.getProperty("skin.scale_factor", "1.0"));
			// e.g. 1.74 -> 1.75
			scaleFactorValue = Math.round(scaleFactorValue * 200.0f) / 2.0f;
			put("value", (int)scaleFactorValue);
		}});
		list.add(new HashMap<String, Object>() {{
			put("id", "opacity");
			put("type", "Spinner");
			put("label", Main.getString("skin.opacity"));
			put("min", 5);
			put("max", 100);
			put("step", 5);
			put("msgTag","gui:change-character-opacity");
			double opacity = Float.parseFloat(Main.getProperty("skin.opacity", "1.0"));
			opacity = Math.round(opacity * 200.0f) / 2.0f;
			put("value", (int)opacity);
		}});		
		list.add(new HashMap<String, Object>() {{
			put("id", "balloon_font");
			put("type", "Button");
			put("label", Main.getString("balloon_font"));
			put("value", Balloon.getDefaultFont().getFamily() + ", " + Balloon.getDefaultFont().getSize());
		}});
		list.add(new HashMap<String, Object>() {{
			put("id", "layer_mode");
			put("type", "ComboBox");
			put("hint",Main.getString("help.layer_mode"));
			put("label", Main.getString("character.layer_mode"));
			List<Object> values=FXCollections.observableList(Arrays.asList((Object[])Character.LayerMode.values()));
			int sel=-1;
			for(Object value : values){
				sel++;
				if(value.equals(App.getInstance().getCharacter().getLayerMode())) break;
			}
			put("msgTag","gui:change-layer-mode");
			put("values", values);
			put("value", sel);
		}});
		list.add(new HashMap<String, Object>() {{
			put("id", "balloon_default_timeout");
			put("type", "Spinner");
			put("label", Main.getString("balloon_default_timeout"));
			put("min", 0);
			put("max", 2000);
			put("step", 50);
			put("msgTag","gui:change-balloon-timeout");
			put("value", Integer.parseInt(Main.getProperty("balloon.default_timeout", "200")));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id", "balloon_position_mode");
			put("type", "ComboBox");
			put("hint",Main.getString("help.balloon_position"));
			put("label", Main.getString("balloon_position_mode"));
			List<Object> values=FXCollections.observableList(Arrays.asList((Object[])Balloon.PositionMode.values()));
			int sel=-1;
			for(Object value : values){
				sel++;
				if(value.equals(App.getInstance().getCharacter().getBalloonPositionMode())) break;
			}
			put("msgTag","gui:change-balloon-position-mode");
			put("values", values);
			put("value", sel);
		}});
        list.add(new HashMap<String, Object>() {{
			put("id", "opacity");
			put("type", "Spinner");
			put("label", Main.getString("balloon.opacity"));
			put("min", 5);
			put("max", 100);
			put("step", 5);
			put("msgTag","gui:change-balloon-opacity");
			double opacity = Float.parseFloat(Main.getProperty("balloon.opacity", "1.0"));
			opacity = Math.round(opacity * 200.0f) / 2.0f;
			put("value", (int)opacity);
		}});

		list.add(new HashMap<String, Object>() {{
			put("id", "enable_context_menu");
			put("type", "CheckBox");
			put("hint",Main.getString("help.context_menu"));
			put("label", Main.getString("enable_context_menu"));
			put("value", Main.getProperty("character.enable_context_menu", "1").equals("1"));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id", "load_resource_pack");
			put("type", "Button");
			put("hint", Main.getString("help.resources"));
			put("label", Main.getString("load_resource_pack"));
			put("value", Main.getString("load"));
		}});
		ControlsContainer poTab = new ControlsContainer(() -> instance.getDialogPane().getScene().getWindow(), Main.getString("appearance"), list, null);
		tabPane.getTabs().add(new Tab(poTab.name, poTab.createControlsPane()));
	}
	private void initCommandsTab(){
		BorderPane commandTab = new BorderPane();
		commandTab.setCenter(commandsTable);
		commandTab.setPrefSize(400, 300);

		commandsTable.setEditable(true);
		commandsTable.setPlaceholder(new Label(Main.getString("commands.empty")));

		TableColumn eventCol = new TableColumn(Main.getString("events"));
		eventCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("event"));
		eventCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(),FXCollections.observableArrayList(CommandsProxy.getEventsList())));
		eventCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setEvent(event.getNewValue());
		});
		eventCol.setMinWidth(120);

		TableColumn commandCol = new TableColumn(Main.getString("commands"));
		commandCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("command"));
		commandCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(),FXCollections.observableArrayList(CommandsProxy.getCommandsList())));
		commandCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setCommand(event.getNewValue());
		});
		commandCol.setMinWidth(120);

		TableColumn ruleCol = new TableColumn(Main.getString("rules"));
		ruleCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("rule"));
		ruleCol.setCellFactory(TextFieldTableCell.<CommandItem> forTableColumn());
		ruleCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setRule(event.getNewValue());
		});
		ruleCol.setMinWidth(120);

		TableColumn msgCol = new TableColumn(Main.getString("parameters"));
		msgCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("msgData"));
		msgCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setMsgData(event.getNewValue());
		});
		msgCol.setMinWidth(120);

		ObservableList<CommandItem> list=FXCollections.observableArrayList();
		for(Map<String,Object> entry : CommandsProxy.getLinksList()){
			list.add(new CommandItem(entry));
		}

		commandsTable.setItems(list);

		commandsTable.getColumns().addAll(eventCol,commandCol,ruleCol,msgCol);

		Button deleteButton = new Button(Main.getString("delete"));
		deleteButton.setOnAction(event -> {
			if(commandsTable.getSelectionModel().getSelectedIndex()>=0)
				commandsTable.getItems().remove(commandsTable.getSelectionModel().getSelectedIndex());
		});
		Button saveButton = new Button(Main.getString("save"));
		saveButton.setOnAction(event -> {
			ArrayList<Map<String,Object>> push=new ArrayList<>();
			for(CommandItem item : commandsTable.getItems()){
				push.add(item.toMap());
			}
			CommandsProxy.resetLinks(push);
			CommandsProxy.save();
		});
		Button loadButton = new Button(Main.getString("load"));
		loadButton.setOnAction(event -> {
			CommandsProxy.load();
			ObservableList<CommandItem> l=FXCollections.observableArrayList();
			for(Map<String,Object> entry : CommandsProxy.getLinksList()){
				l.add(new CommandItem(entry));
			}
			commandsTable.setItems(l);
		});
		Button addButton=new Button(Main.getString("add"));
		addButton.setOnAction(event -> {
			CommandItem item=new CommandItem(CommandsProxy.getEventsList().get(0),CommandsProxy.getCommandsList().get(0),"","");
			commandsTable.getItems().add(item);
		});
		HBox buttons=new HBox(addButton,deleteButton,loadButton,saveButton);
		commandTab.setBottom(buttons);
		tabPane.getTabs().add(new Tab(Main.getString("commands"), commandTab));
	}
	private void initTabs() {
		PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();
		GridPane gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");

		/// appearance
		initMainTab();

		/// commands
		initCommandsTab();

		/// plugins
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

		/// alternatives
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

		/// debug
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

		/// plugin's tabs
		for (Map.Entry<String, List<ControlsContainer>> entry : pluginsTabs.entrySet()) {
			for (ControlsContainer tab : entry.getValue()) {
				tabPane.getTabs().add(new Tab(tab.name, tab.createControlsPane()));
			}
		}

		/// about
		gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");
		Label label = new Label(CoreInfo.get("NAME") + " " + CoreInfo.get("VERSION"));
		label.getStyleClass().add("header");
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

		/// appearance set up
		for(Tab tab : getInstance().tabPane.getTabs()) {
			if (!tab.getText().equals(Main.getString("appearance"))) continue;
			GridPane pane = (GridPane) ((BorderPane) tab.getContent()).getChildren().get(0);
			for (Node node : pane.getChildren()) {
				if (node.getId() != null && node.getId().equals("skin"))
					skinManagerButton = (Button) node;
				if (node.getId() != null && node.getId().equals("balloon_font"))
					balloonFontButton = (Button) node;
				if (node.getId() != null && node.getId().equals("enable_context_menu")){
					((CheckBox) node).selectedProperty().addListener((property, oldValue, newValue) -> {
						Main.setProperty("character.enable_context_menu", newValue ? "1" : "0");
					});
				}
				if (node.getId() != null && node.getId().equals("load_resource_pack")) {
					((Button) node).setOnAction(event -> {
						try {
							FileChooser packChooser=new FileChooser();
							packChooser.setInitialDirectory(pluginProxy.getRootDirPath().toFile());
							File f = packChooser.showOpenDialog(getDialogPane().getScene().getWindow());
							pluginProxy.sendMessage("core:distribute-resources", f.toString());
						} catch(Exception e){ }
					});
				}
			}
		}
		balloonFontButton.setOnAction(event -> {
			FontSelectorDialog dialog = new FontSelectorDialog(Balloon.getDefaultFont());
			dialog.initOwner(getDialogPane().getScene().getWindow());
			Optional<Font> selectedFontOpt = dialog.showAndWait();
			if (selectedFontOpt.isPresent()) {
				Font selectedFont = selectedFontOpt.get();
				Balloon.setDefaultFont(selectedFont);
				Main.setProperty("balloon.font.family", selectedFont.getFamily());
				Main.setProperty("balloon.font.size", String.valueOf(selectedFont.getSize()));
				updateInstanceTabs();
			}
		});
		skinManagerButton.setOnAction(event -> openSkinManager());
	}

	protected void openSkinManager() {
		SkinManagerDialog dialog = new SkinManagerDialog(getDialogPane().getScene().getWindow());
		dialog.showAndWait();
		updateInstanceTabs();
		Main.setProperty("skin.name", App.getInstance().getCharacter().getSkin().getName());
	}
	static void registerPluginTab(String plugin, String name, List<Map<String, Object>> controls, String msgTag) {
		List<ControlsContainer> tabs = pluginsTabs.getOrDefault(plugin, null);
		ControlsContainer poTab = new ControlsContainer(() -> instance.getDialogPane().getScene().getWindow(), name, controls, msgTag);
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

	public static class CommandItem{
		String event;
		String command;
		String rule;
		Object msgData;
		CommandItem(String event, String command, String rule, String msg) {
			this.event=event;
			this.command=command;
			this.rule=rule;
			this.msgData=msg;
		}
		CommandItem(Map<String,Object> data) {
			this.event=(String)data.get("event");
			this.command=(String)data.get("command");
			this.rule=(String)data.get("rule");
			this.msgData=data.get("msg");
		}
		public String getEvent(){ return event; }
		public void setEvent(String value){ event=value; }
		public String getCommand(){ return command; }
		public void setCommand(String value){ command=value; }
		public String getRule(){ return rule; }
		public void setRule(String value){ rule=value; }
		public String getMsgData(){ return msgData!=null ? msgData.toString() : ""; }
		public void setMsgData(String value){ msgData=value; }
		public Map<String,Object> toMap(){
			HashMap<String,Object> data=new HashMap<>();
			data.put("eventName",event);
			data.put("commandName",command);
			if(rule!=null && rule.length()>0) data.put("rule",rule);
			if(msgData!=null) data.put("msgData",msgData);
			return data;
		}
	}
}
