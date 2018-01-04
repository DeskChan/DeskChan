package info.deskchan.gui_javafx;

import info.deskchan.core.CommandsProxy;
import info.deskchan.core.CoreInfo;
import info.deskchan.core.PluginManager;
import info.deskchan.core.PluginProxyInterface;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.json.JSONObject;

import java.io.File;
import java.lang.Character;
import java.nio.file.Path;
import java.util.*;

class OptionsDialog extends TemplateBox {

	private static OptionsDialog instance = null;

	/** Top tabs container. **/
	private TabPane tabPane = new TabPane();

	/** Tab list list. **/
	private ListView<String> tabListView = new ListView<>();
	
	/** Skin options sub menu, 'Appearance' tab. **/
	private ControlsPane skinOptions;

	/** Balloon options sub menu, 'Appearance' tab. **/
	private ControlsPane balloonOptions;

	/** List of rows representing registered plugins in the core, 'Plugins' tab. **/
	private ListView<PluginListItem> pluginsList = new ListView<>();

	/** Table of alternatives registered in the core, 'Alternatives' tab. **/
	private TreeTableView<AlternativeTreeItem> alternativesTable = new TreeTableView<>();

	/** Table of rows representing commands registered in the core, 'Commands' tab. **/
	private TableView<CommandItem> commandsTable=new TableView<>();

	/** All tabs registered by plugins, plugin -> list of tabs. **/
	private static Map<String, List<ControlsPane>> pluginsTabs = new HashMap<>();

	/** All submenus registered by plugins, plugin -> list of menus. **/
	private static Map<String, List<ControlsPane>> pluginsSubMenus = new HashMap<>();

	static {
		// Filling plugins list, 'plugin-load' sends full list of registered commands every time you subscribed to it
		Main.getPluginProxy().addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (instance == null) return;
				for (PluginListItem item : instance.pluginsList.getItems()) {
					if (item.id.equals(data)) {
						return;
					}
				}
				instance.pluginsList.getItems().add(new PluginListItem(data.toString(), false));
			});
		});

		// Listener to unloaded plugins to remove them from list
		Main.getPluginProxy().addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (instance == null) return;
				instance.pluginsList.getItems().removeIf(item -> item.id.equals(data) && !item.blacklisted);
			});
		});

	}

	OptionsDialog() {
		super(Main.getString("deskchan_options"));
		instance = this;
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabPane.setTabMaxWidth(0);
		tabPane.setTabMaxHeight(0);
		tabPane.setSide(Side.RIGHT);
		tabPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("right"), false);
		tabPane.pseudoClassStateChanged(PseudoClass.getPseudoClass(TabPane.STYLE_CLASS_FLOATING), true);
		tabListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		initTabs();
		syncTabListViewItems();
		tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> syncTabListViewItems());
		tabListView.getSelectionModel().selectedIndexProperty().addListener((observableValue, oldValue, newValue) ->
				tabPane.getSelectionModel().select(newValue.intValue()));
		HBox contentPane = new HBox();
		contentPane.getChildren().add(tabListView);
		contentPane.getChildren().add(tabPane);
		/* !!! FIXME: Begin of dirty code !!! */
		HBox.setMargin(tabPane, new Insets(0, -13, 0, 6));
		Rectangle clipRect = new Rectangle();
		clipRect.widthProperty().bind(contentPane.widthProperty().subtract(13));
		clipRect.heightProperty().bind(contentPane.heightProperty());
		contentPane.setClip(clipRect);
		/* !!! End of dirty code !!! */
		getDialogPane().setContent(contentPane);
		setOnHidden(event -> {
			instance = null;
		});
	}
	
	private void syncTabListViewItems() {
		tabListView.getItems().clear();
		for (Tab tab : tabPane.getTabs()) {
			tabListView.getItems().add(tab.getText());
		}
		tabListView.getSelectionModel().select(tabPane.getSelectionModel().getSelectedIndex());
	}
	
	static OptionsDialog getInstance() {
		return instance;
	}

	/** Creating 'Appearance' tab. **/
	public void initMainTab(){
		List<Map<String, Object>> list = new LinkedList<>();
		list.add(new HashMap<String, Object>() {{
			put("id",    "character_options");
			put("type",  "Button");
			put("label",  Main.getString("skin.options"));
			put("value",  Main.getString("open"));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon_options");
			put("type",  "Button");
			put("label",  Main.getString("balloon.options"));
			put("value",  Main.getString("open"));
		}});
		/*list.add(new HashMap<String, Object>() {{
			put("id",    "interface_size");
			put("type",  "Slider");
			put("min",    0.1);
			put("max",    5);
			put("msgTag","gui:set-interface-size");
			put("label",  Main.getString("interface_size"));
			put("value",  App.getInterfaceMultiplierSize());
		}});*/
		list.add(new HashMap<String, Object>() {{
			put("id",    "interface_font");
			put("type",  "FontPicker");
			put("msgTag","gui:set-interface-font");
			put("label",  Main.getString("interface_font"));
			put("value",  LocalFont.defaultToString());
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "interface.on_top");
			put("type",  "CheckBox");
			put("msgTag","gui:switch-interface-front");
			put("label",  Main.getString("interface.on_top"));
			put("value",  TemplateBox.checkForceOnTop());
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "layer_mode");
			put("type",  "ComboBox");
			put("hint",   Main.getString("help.layer_mode"));
			put("label",  Main.getString("character.layer_mode"));
			List<Object> values=FXCollections.observableList(new ArrayList<>());
			values.addAll(OverlayStage.getStages());
			int sel=-1;
			OverlayStage.LayerMode mode = OverlayStage.getCurrentStage();
			for(Object value : values){
				sel++;
				if(value.equals(mode)) break;
			}
			put("msgTag","gui:change-layer-mode");
			put("values", values);
			put("value",  sel);
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "enable_context_menu");
			put("type",  "CheckBox");
			put("hint",   Main.getString("help.context_menu"));
			put("label",  Main.getString("enable_context_menu"));
			put("value",  Main.getProperties().getBoolean("character.enable_context_menu", true));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "load_resource_pack");
			put("type",  "Button");
			put("hint",   Main.getString("help.resources"));
			put("label",  Main.getString("load_resource_pack"));
			put("value",  Main.getString("load"));
		}});
		ControlsPane poTab = new ControlsPane(Main.getString("appearance"), list, null, null);
		tabPane.getTabs().add(new Tab(poTab.name, poTab.createControlsPane(instance)));
	}

	/** Creating 'Skin' options submenu. **/
	private ControlsPane characterOptions(){
		List<Map<String, Object>> list = new LinkedList<>();
		list.add(new HashMap<String, Object>() {{
			put("id",    "skin");
			put("type",  "Button");
			put("label",  Main.getString("skin"));
			put("hint",   Main.getString("help.skin"));
			put("msgTag","gui:open-skin-dialog");
			put("value",  App.getInstance().getCharacter().getSkin().toString());
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "scale");
			put("type",  "Spinner");
			put("label",  Main.getString("scale_factor") + " (%)");
			put("min",    10);
			put("max",    1000);
			put("msgTag","gui:resize-character");
			put("value",   Main.getProperties().getInteger("skin.scale_factor", 100));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "skin-opacity");
			put("type",  "Slider");
			put("label",  Main.getString("skin.opacity") + " (%)");
			put("min",    0);
			put("max",    100);
			put("msgTag","gui:change-skin-opacity");
			put("value",  Main.getProperties().getInteger("skin.opacity", 100));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "skin-shadow-opacity");
			put("type",  "Slider");
			put("label",  Main.getString("shadow-opacity") + " (%)");
			put("min",    0);
			put("max",    100);
			put("msgTag","gui:set-skin-shadow-opacity");
			put("value",  Main.getProperties().getInteger("skin.shadow-opacity", 100));
		}});
		skinOptions = new ControlsPane(Main.getString("skin"), list, null, null);
		return skinOptions;
	}

	/** Creating 'Balloon' options submenu. **/
	private ControlsPane balloonOptions(){
		List<Map<String, Object>> list = new LinkedList<>();
		list.add(new HashMap<String, Object>() {{
			put("id",    "scale");
			put("type",  "Spinner");
			put("label",  Main.getString("scale_factor") + " (%)");
			put("min",    10);
			put("max",    1000);
			put("msgTag","gui:resize-balloon");
			put("value",  Main.getProperties().getInteger("balloon.scale_factor", 100));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon_font");
			put("type",  "FontPicker");
			put("msgTag","gui:set-balloon-font");
			put("label",  Main.getString("balloon_font"));
			put("value",  LocalFont.toString(Balloon.getDefaultFont()));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon_default_timeout");
			put("type",  "Spinner");
			put("label",  Main.getString("balloon_default_timeout"));
			put("min",    0);
			put("max",    2000);
			put("msgTag","gui:change-balloon-timeout");
			put("value",  Main.getProperties().getInteger("balloon.default_timeout", 200));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon_position_mode");
			put("type",  "ComboBox");
			put("hint",   Main.getString("help.balloon_position"));
			put("label",  Main.getString("balloon_position_mode"));
			List<Object> values =
					FXCollections.observableList( Arrays.asList((Object[]) Balloon.PositionMode.values()) );
			int sel=-1;
			for(Object value : values){
				sel++;
				if(value.equals(App.getInstance().getCharacter().getBalloonPositionMode()))
					break;
			}
			put("msgTag","gui:change-balloon-position-mode");
			put("values", values);
			put("value",  sel);
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon-opacity");
			put("type",  "Slider");
			put("label",  Main.getString("balloon.opacity") + " (%)");
			put("min",    0);
			put("max",    100);
			put("msgTag","gui:change-balloon-opacity");
			put("value",   Main.getProperties().getDouble("balloon.opacity", 100));
		}});
		list.add(new HashMap<String, Object>(){{
			put("id",    "balloon-text-animation");
			put("type",  "CheckBox");
			put("label",  Main.getString("balloon.text-animation"));
			put("msgTag","gui:switch-balloon-animation");
			put("hint",   Main.getString("help.text-animation"));
			put("value",  Main.getProperties().getBoolean("balloon.text-animation", true));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon-text-animation-delay");
			put("type",  "Spinner");
			put("label",  Main.getString("balloon.text-animation-delay"));
			put("min",    1);
			put("max",    1000);
			put("msgTag","gui:switch-balloon-animation");
			put("value",  Main.getProperties().getInteger("balloon.text-animation-delay", 50));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon-shadow-opacity");
			put("type",  "Slider");
			put("label",  Main.getString("shadow-opacity") + " (%)");
			put("min",    0);
			put("max",    100);
			put("msgTag","gui:set-balloon-shadow-opacity");
			put("value",  Main.getProperties().getInteger("balloon.shadow-opacity", 100));
		}});
		balloonOptions = new ControlsPane(Main.getString("balloon"), list, null, null);
		return balloonOptions;
	}

	/** Creating 'Commands' tab. **/
	private void initCommandsTab(){
		BorderPane commandTab = new BorderPane();
		commandTab.setCenter(commandsTable);
		commandTab.setPrefSize(400, 300);

		commandsTable.setEditable(true);
		commandsTable.setPlaceholder(new Label(Main.getString("commands.empty")));

		// Events column
		TableColumn eventCol = new TableColumn(Main.getString("events"));
		eventCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("event"));
		List events = CommandsProxy.getEventsList();
		Collections.sort(events);
		eventCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(),
				FXCollections.observableArrayList(events)));
		eventCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setEvent(event.getNewValue());
		});
		eventCol.setMinWidth(120);

		// Commands column
		TableColumn commandCol = new TableColumn(Main.getString("commands"));
		commandCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("command"));
		List commands = CommandsProxy.getCommandsList();
		Collections.sort(commands);
		commandCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(),
				FXCollections.observableArrayList(commands)));
		commandCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setCommand(event.getNewValue());
		});
		commandCol.setMinWidth(120);

		// Rules column
		TableColumn ruleCol = new TableColumn(Main.getString("rules"));
		ruleCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("rule"));
		ruleCol.setCellFactory(TooltippedTableCell.<CommandItem> forTableColumn());
		ruleCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setRule(event.getNewValue());
		});
		ruleCol.setMinWidth(120);

		// Parameters column
		TableColumn msgCol = new TableColumn(Main.getString("parameters"));
		msgCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("msgData"));
		msgCol.setCellFactory(TooltippedTableCell.<CommandItem> forTableColumn());
		msgCol.setOnEditCommit(ev -> {
			TableColumn.CellEditEvent<CommandItem, String> event=(TableColumn.CellEditEvent<CommandItem, String>) ev;
			CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
			item.setMsgData(event.getNewValue());
		});
		msgCol.setMinWidth(120);

		// Filling list of commands
		ObservableList<CommandItem> list = FXCollections.observableArrayList();
		for(Map<String,Object> entry : CommandsProxy.getLinksList()){
			list.add(new CommandItem(entry));
		}

		commandsTable.setItems(list);

		// Setting columns
		commandsTable.getColumns().addAll(eventCol, ruleCol, commandCol, msgCol);

		// 'Delete' button
		Button deleteButton = new Button(Main.getString("delete"));
		deleteButton.setOnAction(event -> {
			if(commandsTable.getSelectionModel().getSelectedIndex()>=0)
				commandsTable.getItems().remove(commandsTable.getSelectionModel().getSelectedIndex());
		});

		// 'Reset' button
		Button resetButton = new Button(Main.getString("reset"));
		resetButton.setOnAction(event -> {
			CommandsProxy.reset();
			ObservableList<CommandItem> l = FXCollections.observableArrayList();
			for(Map<String,Object> entry : CommandsProxy.getLinksList()){
				l.add(new CommandItem(entry));
			}
			commandsTable.setItems(l);
		});

		// 'Save' button
		Button saveButton = new Button(Main.getString("save"));
		saveButton.setOnAction(event -> {
			ArrayList<Map<String,Object>> push = new ArrayList<>();
			for(CommandItem item : commandsTable.getItems()){
				push.add(item.toMap());
			}
			CommandsProxy.setLinks(push);
			CommandsProxy.save();
		});

		// 'Load' button
		Button loadButton = new Button(Main.getString("load"));
		loadButton.setOnAction(event -> {
			CommandsProxy.load();
			ObservableList<CommandItem> l=FXCollections.observableArrayList();
			for(Map<String,Object> entry : CommandsProxy.getLinksList()){
				l.add(new CommandItem(entry));
			}
			commandsTable.setItems(l);
		});

		// 'Add' button
		Button addButton=new Button(Main.getString("add"));
		addButton.setOnAction(event -> {
			CommandItem item=new CommandItem(CommandsProxy.getEventsList().get(0),CommandsProxy.getCommandsList().get(0),"","");
			commandsTable.getItems().add(item);
		});

		// Adding buttons to form
		HBox buttons = new HBox(addButton, deleteButton, loadButton, saveButton, resetButton);
		commandTab.setBottom(buttons);

		tabPane.getTabs().add(new Tab(Main.getString("commands"), commandTab));
	}

	private void initTabs() {
		PluginProxyInterface pluginProxy = Main.getInstance().getPluginProxy();
		GridPane gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");
		double prefWidth  = 400 * App.getInterfaceMultiplierSize(),
			   prefHeight = 300 * App.getInterfaceMultiplierSize();


		/// appearance
		initMainTab();


		/// commands
		initCommandsTab();


		/// plugins
		BorderPane pluginsTab = new BorderPane();
		pluginsTab.setCenter(pluginsList);
		pluginsList.setPrefSize(prefWidth, prefHeight);

		// Setting row style
		pluginsList.setCellFactory(new Callback<ListView<PluginListItem>, ListCell<PluginListItem>>(){
			@Override
			public ListCell<PluginListItem> call(ListView<PluginListItem> obj) {
				ListCell<PluginListItem> cell = new ListCell<PluginListItem>(){
					@Override
					protected void updateItem(PluginListItem t, boolean bln) {
						super.updateItem(t, bln);
						//setStyle("-fx-cell-size: " + LocalFont.defaultFont.getSize()*2.5);
						if (t != null) {
							setGraphic(t.hbox);
							setTooltip(t.tooltip);
						} else {
							setTooltip(null);
						}
					}
				};
				return cell;
			}
		});

		for (String id : PluginManager.getInstance().getPlugins()) {
			pluginsList.getItems().add(new PluginListItem(id, false));
		}

		// Blacklisted plugins that not registered in program
		for (String id : PluginManager.getInstance().getBlacklistedPlugins()) {
			pluginsList.getItems().add(new PluginListItem(id, true));
		}

		// Load button
		HBox hbox = new HBox();
		Button button = new Button(Main.getString("load"));
		button.setOnAction(event -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle(Main.getString("load_plugin"));
			chooser.setInitialDirectory(PluginManager.getPluginsDirPath().toFile());
			File file = chooser.showOpenDialog(OptionsDialog.this.getDialogPane().getScene().getWindow());
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
		pluginsTab.setBottom(hbox);
		tabPane.getTabs().add(new Tab(Main.getString("plugins"), pluginsTab));


		/// alternatives
		BorderPane alternativesTab = new BorderPane();
		alternativesTab.setCenter(alternativesTable);
		alternativesTable.setPrefSize(prefWidth, prefHeight);
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
			Map<String, List<Map<String, Object>>> map = (Map) data;
			final TreeItem<AlternativeTreeItem> root = new TreeItem<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : map.entrySet()) {
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
				Main.getInstance().getPluginProxy().sendMessage(tag, stringToMap(dataStr));
			} catch (Throwable e) {
				App.showThrowable(OptionsDialog.this.getDialogPane().getScene().getWindow(), e);
			}
		});

		Button reloadButton = new Button(Main.getString("reload-style"));
		button.setOnAction(event -> {
			instance.applyStyle();
			instance.hide();
			instance.show();
		});

		debugTab.setBottom(new HBox(button, reloadButton));
		tabPane.getTabs().add(new Tab(Main.getString("debug"), debugTab));


		/// Creating top tabs from registered tabs list
		for (Map.Entry<String, List<ControlsPane>> entry : pluginsTabs.entrySet()) {
			for (ControlsPane tab : entry.getValue()) {
				tabPane.getTabs().add(new Tab(tab.name, tab.createControlsPane(instance)));
			}
		}


		/// about
		gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");
		Label label = new Label(CoreInfo.get("NAME") + " " + CoreInfo.get("VERSION"));
		label.setFont(new Font(LocalFont.defaultFont.getName(), LocalFont.defaultFont.getSize() * 2));
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
		gridPane.add(new Label(Main.getString("Language")), 0, 5);
		ComboBox<String> locales=new ComboBox<>();
		for(Map.Entry<String,String> locale : CoreInfo.locales.entrySet()){
			locales.getItems().add(locale.getValue());
			if(Locale.getDefault().getLanguage().equals(locale.getKey()))
				locales.getSelectionModel().select(locale.getValue());
		}
		locales.valueProperty().addListener( (obj,oldValue,newValue) -> {
			for(Map.Entry<String,String> locale : CoreInfo.locales.entrySet()){
				if(locale.getValue().equals(newValue)){
					TemplateBox dialog = new TemplateBox(Main.getString("default_messagebox_name"));
					dialog.setContentText(Main.getString("info.restart"));
					dialog.requestFocus();
					dialog.show();
					Locale.setDefault(new Locale(locale.getKey()));
					Main.getProperties().put("locale", locale.getKey());
					break;
				}
			}
		});
		gridPane.add(locales, 1, 5);
		tabPane.getTabs().add(new Tab(Main.getString("about"), gridPane));


		/// Creating additional listeners for 'Appearance' tab, must be after tabs initialization
		for(Tab tab : getInstance().tabPane.getTabs()) {
			if (!tab.getText().equals(Main.getString("appearance"))) continue;
			GridPane pane = (GridPane) ((BorderPane) tab.getContent()).getChildren().get(0);
			for (Node node : pane.getChildren()) {
				if (node.getId() != null && node.getId().equals("enable_context_menu")){
					((CheckBox) node).selectedProperty().addListener((property, oldValue, newValue) -> {
						Main.getProperties().put("character.enable_context_menu", newValue);
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
				if (node.getId() != null && node.getId().equals("balloon_options")){
					((Button) node).setOnAction(event -> {
						ControlsWindow.setupCustomWindow(Main.getInstance().getPluginProxy().getId(), balloonOptions());
					});
				}
				if (node.getId() != null && node.getId().equals("character_options")){
					((Button) node).setOnAction(event -> {
						ControlsWindow.setupCustomWindow(Main.getInstance().getPluginProxy().getId(), characterOptions());
					});
				}
			}
		}
	}

	/** Open skin manager and wait for closing. **/
	public void openSkinManager() {
		SkinManagerDialog dialog = new SkinManagerDialog(getDialogPane().getScene().getWindow());
		dialog.showAndWait();

		// Update skin name in menu
		Main.getProperties().put("skin.name", App.getInstance().getCharacter().getSkin().getName());
		List<Map<String, Object>> list = new ArrayList<>();
		list.add(new HashMap<String, Object>() {{
			put("id",    "skin");
			put("value",  App.getInstance().getCharacter().getSkin().toString());
		}});
		skinOptions.updateControlsPane(list);
	}

	protected static void openSubMenu(String pluginId, String menuId) {
		try {
			for (ControlsPane container : pluginsSubMenus.get(pluginId)) {
				if (container.name.equals(menuId)) {
					ControlsWindow.setupCustomWindow(pluginId, container);
				}
			}
		} catch (Exception e){
			Main.log(e);
		}
	}

	/** Register plugin menu, will be shown in options. Use updatePluginMenu to update its content.
	 * @param plugin plugin name
	 * @param data content of menu
	 * @param isTab True - will be shown as top tab, False - will be shown as submenu button in 'Plugins' tab
	 */
	static void registerPluginMenu(String plugin, Map<String, Object> data, boolean isTab) {
		Map<String, List<ControlsPane>> menu = isTab ? pluginsTabs : pluginsSubMenus;
		List<ControlsPane> tabs = menu.get(plugin);

		String name = (String) data.getOrDefault("name", plugin);
		List<Map<String, Object>> controls = (List) data.getOrDefault("controls", new LinkedList<>());
		String msgTag = (String) data.get("msgTag");
		String msgClose = (String) data.get("onClose");

		ControlsPane pluginMenuContainer = new ControlsPane(name, controls, msgTag, msgClose);

		if (tabs == null) {  // No menus registered by plugin
			tabs = new ArrayList<>();
			menu.put(plugin, tabs);
			tabs.add(pluginMenuContainer);
			return;
		} else {
			boolean found = false;
			for (int i = 0; i < tabs.size(); i++) {
				if (tabs.get(i).name.equals(name)) {
					tabs.set(i, pluginMenuContainer);
					found = true;
					break;
				}
			}
			if (!found) tabs.add(pluginMenuContainer);
		}

		/// Is options is currently shown, we update its content
		if (instance == null) return;
		if (isTab) {
			for (Tab tab : instance.tabPane.getTabs()) {
				if (tab.getText().equals(name)) {
					tab.setContent(pluginMenuContainer.createControlsPane(instance));
					break;
				}
			}
		} else {
			for(PluginListItem pli : instance.pluginsList.getItems()){
				if(pli.id.equals(plugin))
					pli.updateOptionsSubMenu();
			}

		}
	}

	/** Register plugin menu, will be shown in options. Use updatePluginMenu to update its content.
	 * @param plugin plugin name
	 * @param data content of menu
	 * @param isTab True - will be shown as top tab, False - will be shown as submenu button in 'Plugins' tab
	 */
	static void updatePluginMenu(String plugin, Map<String, Object> data, boolean isTab) {
		Map<String, List<ControlsPane>> menu = isTab ? pluginsTabs : pluginsSubMenus;

		List<ControlsPane> tabs = menu.get(plugin);
		String name = (String) data.getOrDefault("name", plugin);
		List<Map<String, Object>> controls = (List) data.getOrDefault("controls", new LinkedList<>());
		String msgTag   = (String) data.get("msgTag");
		String msgClose = (String) data.get("onClose");

		if (tabs == null) return;

		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).name.equals(name)) {
				tabs.get(i).updateControlsPane(controls);
				return;
			}
		}
	}

	/** Unregister all menus registered by plugin
	 * @param plugin plugin name
	 */
	static void unregisterPluginMenus(String plugin) {
		pluginsTabs.remove(plugin);
		pluginsSubMenus.remove(plugin);
	}

	// -- Technical classes --

	/** Class representing row in 'Plugins' tab. **/
	private static class PluginListItem {

		/** List of plugins cannot be simply removed. **/
		private static final String[] importantPlugins=new String[]{
				"core", "core_utils", Main.getInstance().getPluginProxy().getId()
		};

		String id;
		boolean blacklisted;
		HBox hbox = new HBox();
		HBox menuBox = new HBox();
		Button blacklistPluginButton;
		Label label;
		Tooltip tooltip;
		Pane pane = new Pane();
		String locked   = "🔒";
		String unlocked = "🔓";

		PluginListItem(String id, boolean blacklisted) {
			this.id = id;
			this.blacklisted = blacklisted;
			setInfo();
		}

		void setInfo(){
			// Adding char in circle to plugin name if we know its type
			label = new Label(toString());
			label.setAlignment(Pos.CENTER_LEFT);

			// Filling row content
			hbox.getChildren().clear();
			hbox.getChildren().addAll(label, pane, menuBox);

			// Adding tooltip with plugin information
			try {
				tooltip = new Tooltip(PluginManager.getInstance().getPluginConfig(id).getShortDescription());
			} catch (Exception e){
				tooltip = new Tooltip(Main.getString("no-info"));
			}

			try {
				final String description = PluginManager.getInstance().getPluginConfig(id).getDescription();
				if(description != null) {
					Button infoPluginButton = new Button("?");
					infoPluginButton.setTooltip(new Tooltip(Main.getString("info.plugin-info")));
					infoPluginButton.setOnAction(event -> {
						App.showNotification(Main.getString("info"),description);
					});
					hbox.getChildren().add(infoPluginButton);
				}
			} catch (Exception e){ }


			// 'Unload' button
			Button unloadPluginButton = new Button("X");
			unloadPluginButton.setTooltip(new Tooltip(Main.getString("info.unload-plugin")));
			unloadPluginButton.setOnAction(event -> {
				for(String pluginId : importantPlugins){
					if(pluginId.equals(id)){
						if (alert())
							PluginManager.getInstance().unloadPlugin(id);
						return;
					}
				}
				PluginManager.getInstance().unloadPlugin(id);
			});

			PluginListItem item = this;

			// 'Blacklist' button
			blacklistPluginButton = new Button(blacklisted ? locked : unlocked);
			blacklistPluginButton.setTooltip(new Tooltip(Main.getString("info.blacklist-plugin")));
			blacklistPluginButton.setOnAction(event -> {
				for(String plname : importantPlugins) {
					if (plname.equals(id)) {
						if (alert())
							item.toggleBlacklisted();
						return;
					}
				}
				item.toggleBlacklisted();
			});

			hbox.getChildren().addAll(blacklistPluginButton, unloadPluginButton);
			HBox.setHgrow(pane, Priority.ALWAYS);

			updateOptionsSubMenu();
		}

		/** Update all submenus content. **/
		void updateOptionsSubMenu(){
			List<ControlsPane> list = pluginsSubMenus.get(id);
			menuBox.getChildren().clear();
			if(list == null) return;
			for(ControlsPane container : list){
				Button button = new Button(container.name);
				button.setOnAction((event) -> {
					ControlsWindow.setupCustomWindow(id, container);
				});
				menuBox.getChildren().add(button);
				ControlsWindow.updateCustomWindow(id, container);
			}
		}

		/** Toggle blacklisting of plugin. **/
		void toggleBlacklisted(){
			blacklisted = !blacklisted;
			if (blacklisted) {
				PluginManager.getInstance().addPluginToBlacklist(id);
				blacklistPluginButton.setText(locked);
			} else {
				PluginManager.getInstance().removePluginFromBlacklist(id);
				PluginManager.getInstance().tryLoadPluginByName(id);
				blacklistPluginButton.setText(unlocked);
				setInfo();
			}
			label.setText(toString());
			menuBox.setVisible(!blacklisted);
		}

		@Override
		public String toString() {
			return getPluginTypeLetter() + id + (blacklisted ? (" ["+Main.getString("blacklisted")+"]") : "");
		}

		/** Alert about removing important plugin. **/
		private static boolean alert(){
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
			stage.setAlwaysOnTop(TemplateBox.checkForceOnTop());
			alert.setTitle(Main.getString("default_messagebox_name"));
			alert.setContentText(Main.getString("info.shutdown_important"));
			Optional<ButtonType> result = alert.showAndWait();
			return (result.get() == ButtonType.OK);
		}

		private String getPluginTypeLetter(){
			try {
				Object type = PluginManager.getInstance().getPluginConfig(id).get("type");
				Character c = Character.toLowerCase(type.toString().charAt(0));
				return (char) ((int) c - 97 + 9398) + " ";
			} catch (Exception e) {
				return "";
			}
		}
	}

	/** Tree of alternatives for single tag for 'Alternatives' tab. **/
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

	/** Class representing command in 'Commands' tab. **/
	public static class CommandItem {

		String event;
		String command;
		String rule;
		Object msgData;

		CommandItem(String event, String command, String rule, String msg) {
			this.event = event;
			this.command = command;
			this.rule = rule;
			this.msgData = msg;
		}

		CommandItem(Map<String,Object> data) {
			this.event = (String) data.get("event");
			this.command = (String) data.get("command");
			this.rule = (String) data.get("rule");
			this.msgData = data.get("msgData");
		}

		public String getEvent(){         return event;            }
		public void   setEvent(String value){    event = value;    }

		public String getCommand(){       return command;          }
		public void   setCommand(String value){  command = value;  }

		public String getRule(){          return rule;             }
		public void   setRule(String value){     rule = value;     }

		public String getMsgData(){       return msgData != null ? msgData.toString() : ""; }
		public void   setMsgData(String value){
			try {
				msgData = stringToMap(value);
			} catch (Exception e){
				msgData = value;
			}
		}

		public Map<String,Object> toMap(){
			HashMap <String,Object> data = new HashMap<>();

			data.put("eventName", event);
			data.put("commandName", command);
			if(rule != null && rule.length() > 0)  data.put("rule", rule);
			if(msgData != null) data.put("msgData", msgData);

			return data;
		}
	}

	/** Tooltipped cell for 'Commands' tab. **/
	static class TooltippedTableCell<S, T> extends TextFieldTableCell<S, T> {
		public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
			return forTableColumn(new DefaultStringConverter());
		}
		public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(final StringConverter<T> converter) {
			return list -> new TooltippedTableCell<>(converter);
		}
		private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
			return converter == null ? cell.getItem() == null ? "" : cell.getItem()
					.toString() : converter.toString(cell.getItem());
		}
		private void updateItem(final Cell<T> cell, final StringConverter<T> converter) {
			if (cell.isEmpty()) {
				cell.setText(null);
				cell.setTooltip(null);
			} else {
				String itemText = getItemText(cell, converter);
				cell.setText(itemText);
				if (itemText.length() == 0) return;
				//Add text as tooltip so that user can read text without editing it.
				Tooltip tooltip = new Tooltip(getItemText(cell, converter));
				tooltip.setWrapText(true);
				tooltip.setMaxWidth(1000);
				tooltip.setMinWidth(300);
				tooltip.prefWidthProperty().bind(cell.widthProperty());
				cell.setTooltip(tooltip);
			}
		}
		private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this, "converter");
		public TooltippedTableCell() {
			this(null);
		}
		public TooltippedTableCell(StringConverter<T> converter) {
			this.getStyleClass().add("tooltipped-table-cell");
			setConverter(converter);
		}
		@Override
		public void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			updateItem(this, getConverter());
		}
	}

	private static Map stringToMap(String text) throws Exception{
		text = text.trim();
		if (!text.startsWith("{")) text = "{" + text;
		if (!text.endsWith("}"))   text = text + "}";

		JSONObject json = new JSONObject(text);
		return json.toMap();
	}
}