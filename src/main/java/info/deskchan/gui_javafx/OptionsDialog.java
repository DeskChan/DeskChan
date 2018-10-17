package info.deskchan.gui_javafx;

import info.deskchan.MessageData.GUI.SetPanel;
import info.deskchan.core.*;
import info.deskchan.core_utils.Browser;
import info.deskchan.gui_javafx.panes.CharacterBalloon;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

class OptionsDialog extends TemplateBox {

	private static OptionsDialog instance = null;

	/** Tab list list. **/
	private ListView<String> tabListView = new ListView<>();

	/** List of rows representing registered plugins in the core, 'Plugins' tab. **/
	private PluginList pluginsList = new PluginList();

	/** Table of alternatives registered in the core, 'Alternatives' tab. **/
	private TreeTableView<AlternativeTreeItem> alternativesTable = new TreeTableView<>();

	private Pane controlsPane = new Pane();

	private static CommandsPane commandsTable;

	private LinkedList<ControlsPanel> panelsHistory = new LinkedList<>();
	private int panelIndex = 0;
	private Hyperlink prevLink, nextLink;

	private void updateLinks(){
		prevLink.setDisable(panelIndex == 0);
		prevLink.setVisited(false);
		nextLink.setDisable(panelIndex == panelsHistory.size() - 1);
		nextLink.setVisited(false);
	}

	static {
		// Filling plugins list, 'plugin-load' sends full list of registered commands every time you subscribed to it
		Main.getPluginProxy().addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (instance == null) return;
				instance.pluginsList.add(new PluginListItem(data.toString(), false));
				EditCommandDialog.reloadInfo();
			});
		});

		// Listener to unloaded plugins to remove them from list
		Main.getPluginProxy().addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			Platform.runLater(() -> {
				if (instance == null) return;
				instance.pluginsList.remove(data.toString());
				EditCommandDialog.reloadInfo();
			});
		});
	}

	OptionsDialog() {
		super("options-window", Main.getString("deskchan_options"));
		instance = this;

		// Left panel

		/// Menu
		tabListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tabListView.setId("options-menu");
		initTabs();

		tabListView.getSelectionModel().selectedIndexProperty().addListener(
				(observableValue, oldValue, newValue) -> {
					String selected = tabListView.getItems().get(newValue.intValue());
					for (ControlsPanel tab : ControlsPanel.getPanels(SetPanel.PanelType.TAB)){
						if (tab.name.equals(selected)) {
							tab.show();
							return;
						}
					}
				}
		);

		/// Info
		Hyperlink siteLink = new Hyperlink(CoreInfo.get("PROJECT_SITE_URL"));
		siteLink.setOnAction(event -> {
			App.getInstance().getHostServices().showDocument(siteLink.getText());
		});

		FlowPane infoPane = new FlowPane();
		infoPane.setId("info-pane");
		infoPane.getChildren().addAll(
				getFlowPane("build-name", new Label(CoreInfo.get("NAME") + " " + CoreInfo.get("VERSION"))),
				getFlowPane("about-git_branch", new Label(Main.getString("about.git_branch")),
						new Label(CoreInfo.get("GIT_BRANCH_NAME"))
				),
				getFlowPane("about-build_datetime", new Label(Main.getString("about.build_datetime")),
						new Label(CoreInfo.get("BUILD_DATETIME"))
				),
				getFlowPane("about-site", new Label(Main.getString("about.site")), siteLink),
				getFlowPane("about-git_commit_hash", new Label(Main.getString("about.git_commit_hash")),
						new ControlsPanel.Hint(CoreInfo.get("GIT_COMMIT_HASH"), Main.getString("open"))
				)
		);
		infoPane.setFocusTraversable(false);

		// Pagination
		FlowPane historyLinks = new FlowPane();
		historyLinks.setId("pagination");
		prevLink = newHyperlink(Main.getString("back"), (event) -> {
			if (panelIndex > 0){
				panelIndex--;
				setPanel(panelsHistory.get(panelIndex));
				tabListView.getSelectionModel().select(panelsHistory.get(panelIndex).name);
			}
			updateLinks();
		});
		prevLink.setId("back");
		prevLink.setDisable(true);

		nextLink = newHyperlink(Main.getString("forward"), (event) -> {
			if (panelIndex < panelsHistory.size() - 1){
				panelIndex++;
				setPanel(panelsHistory.get(panelIndex));
				tabListView.getSelectionModel().select(panelsHistory.get(panelIndex).name);
			}
			updateLinks();
		});
		nextLink.setId("forward");
		nextLink.setDisable(true);

		historyLinks.getChildren().addAll(prevLink, nextLink);

        // Main grid
		StackPane stackPane = new StackPane();
		stackPane.setId("options-grid");
		stackPane.getChildren().addAll(tabListView, historyLinks, infoPane, controlsPane);

		//controlsPane.maxHeightProperty().bind(gridPane.heightProperty());
		controlsPane.setId("controls");

		getDialogPane().setContent(stackPane);

		// Set size
		//getDialogPane().setMinHeight(500 * App.getInterfaceScale());
		//getDialogPane().setMinWidth(900 * App.getInterfaceScale());

		setOnHiding(event -> {
			Main.getPluginProxy().sendMessage("core:save-all-properties", null);
		});
	}

	static OptionsDialog getInstance() {
		return instance;
	}

	/** Creating 'Appearance' tab. **/
	private static void initMainTab(){
		List<Map<String, Object>> list = new LinkedList<>();

		list.add(new HashMap<String, Object>() {{
			put("id",    "character_options");
			put("type",  "Button");
			put("label",  Main.getString("skin.options"));
			put("value",  Main.getString("open"));
			put("dstPanel", Main.getPluginProxy().getId() + "-skin");
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon_options");
			put("type",  "Button");
			put("label",  Main.getString("balloon.options"));
			put("value",  Main.getString("open"));
			put("dstPanel", Main.getPluginProxy().getId() + "-balloon");
		}});
		/*list.add(new HashMap<String, Object>() {{
			put("id",    "interface_size");
			put("type",  "Slider");
			put("min",    0.1);
			put("max",    5);
			put("msgTag","gui:set-interface-size");
			put("label",  Main.getString("interface_size"));
			put("value",  App.getInterfaceScale());
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
			put("id",    "interface-skin");
			put("type",  "AssetsManager");
			put("folder","styles");
			put("acceptedExtensions", Arrays.asList(".css"));
			put("moreURL", "https://forum.deskchan.info/category/9/themes");
			put("label",  Main.getString("interface.path-skin"));
			put("onChange","gui:set-interface-style");
			put("value",  Main.getProperties().getString("interface.path-skin"));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "layer_mode");
			put("type",  "ComboBox");
			put("hint",   Main.getString("help.layer_mode"));
			put("label",  Main.getString("character.layer_mode"));
			int sel = -1;
			OverlayStage.LayerMode mode = OverlayStage.getCurrentStage();
			List<String> values = new ArrayList<>(), valuesNames = new ArrayList<>();
			for(Object value : OverlayStage.getStages()){
				sel++;
				values.add(value.toString());
				valuesNames.add(Main.getString("layer_mode." + value.toString()));
				if(value.equals(mode)) put("value",  sel);
			}
			put("msgTag","gui:change-layer-mode");
			put("values", values);
			put("valuesNames", valuesNames);
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "enable_context_menu");
			put("type",  "CheckBox");
			put("msgTag","gui:toggle-context-menu");
			put("hint",   Main.getString("help.context_menu"));
			put("label",  Main.getString("enable_context_menu"));
			put("value",  Main.getProperties().getBoolean("character.enable_context_menu", true));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "load_character_config");
			put("type",  "AssetsManager");
			put("folder","characters");
			put("moreURL", "https://forum.deskchan.info/category/20/character-configs");
			put("acceptedExtensions", Arrays.asList(".chr"));
			put("hint",   Main.getString("help.character_config"));
			put("label",  Main.getString("load_character_config"));
			put("onChange","core:distribute-resources");
		}});
		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("appearance"), "appearance", SetPanel.PanelType.TAB, list).set();

		characterOptions();
		balloonOptions();
	}

	/** Creating 'Skin' options submenu. **/
	private static void characterOptions(){
		List<Map<String, Object>> list = new LinkedList<>();
		list.add(new HashMap<String, Object>() {{
			put("id",    "skin");
			put("type",  "AssetsManager");
			put("folder","skins");
			put("moreURL", "https://forum.deskchan.info/category/5/skins");
			put("label",  Main.getString("skin"));
			put("hint",   Main.getString("help.skin"));
			put("onChange","gui:change-skin");
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
		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("skin"), "skin", SetPanel.PanelType.PANEL, list).set();
	}

	/** Creating 'Balloon' options submenu. **/
	private static void balloonOptions(){
		List<Map<String, Object>> list = new LinkedList<>();
		list.add(new HashMap<String, Object>() {{
			put("id",    "skin-character");
			put("type",  "AssetsManager");
			put("folder","balloons");
			put("moreURL", "https://forum.deskchan.info/category/8/balloons");
			put("label",  Main.getString("balloon.path-character"));
			put("onChange","gui:set-character-balloon-path");
			put("value",  Main.getProperties().getString("balloon.path-character"));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "skin-user");
			put("type",  "AssetsManager");
			put("folder","balloons");
			put("moreURL", "https://forum.deskchan.info/category/8/balloons");
			put("label",  Main.getString("balloon.path-user"));
			put("onChange","gui:set-user-balloon-path");
			put("value",  Main.getProperties().getString("balloon.path-user"));
		}});
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
			put("value",  Main.getProperties().put("balloon.font", LocalFont.defaultToString()));
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
			int sel = -1;
			String current = Main.getProperties().getString("balloon_position_mode", CharacterBalloon.PositionMode.ABSOLUTE.toString());
			List<String> values = new ArrayList<>(), valuesNames = new ArrayList<>();
			for(Object value : CharacterBalloon.PositionMode.values()){
				sel++;
				values.add(value.toString());
				valuesNames.add(Main.getString("balloon." + value.toString()));
				if(value.toString().equals(current)) put("value",  sel);
			}
			put("msgTag","gui:change-balloon-position-mode");
			put("values", values);
			put("valuesNames", valuesNames);
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "balloon_direction_mode");
			put("type",  "ComboBox");
			put("hint",   Main.getString("help.balloon_direction"));
			put("label",  Main.getString("balloon_direction_mode"));
			int sel = -1;
			String current = Main.getProperties().getString("balloon_direction_mode", CharacterBalloon.DirectionMode.STANDARD_DIRECTION.toString());
			List<String> values = new ArrayList<>(), valuesNames = new ArrayList<>();
			for(Object value : CharacterBalloon.DirectionMode.values()){
				sel++;
				values.add(value.toString());
				valuesNames.add(Main.getString("balloon." + value.toString()));
				if(value.toString().equals(current)) put("value",  sel);
			}
			put("msgTag","gui:change-balloon-direction-mode");
			put("values", values);
			put("valuesNames", valuesNames);
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
		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("balloon"), "balloon", SetPanel.PanelType.PANEL, list).set();
	}

	/** Creating 'Balloon' options submenu. **/
	private static void generalOptions(){
		List<Map<String, Object>> list = new LinkedList<>();
		list.add(new HashMap<String, Object>() {{
			put("id",    "language");
			put("type",  "ComboBox");
			put("msgTag","core:set-language");
			put("label",  Main.getString("language"));
			List<Object> values = FXCollections.observableList(new ArrayList<>());
			for(Map.Entry<String,String> locale : CoreInfo.locales.entrySet()){
				values.add(locale.getValue());
				if(Locale.getDefault().getLanguage().equals(locale.getKey()))
					put("value", values.size()-1);
			}
			put("values", values);
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "redirect-recognition");
			put("type",  "CheckBox");
			put("label",  Main.getString("redirect-recognition"));
			put("msgTag","gui:toggle-redirect-recognition");
			put("value",  Main.getProperties().getBoolean("redirect-recognition", false));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "alternatives");
			put("type",  "Button");
			put("label",  Main.getString("alternatives"));
			put("dstPanel", Main.getPluginProxy().getId()+"-alternatives");
			put("value",  Main.getString("open"));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "debug");
			put("type",  "Button");
			put("label",  Main.getString("debug-window"));
			put("dstPanel", Main.getPluginProxy().getId()+"-debug");
			put("value",  Main.getString("open"));
		}});
		list.add(new HashMap<String, Object>() {{
			put("id",    "open_log");
			put("type",  "Button");
			put("msgTag","gui:open-log-file");
			put("label",  Main.getString("open_log"));
			put("value",  Main.getString("open"));
		}});
		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("general_options"), "general", SetPanel.PanelType.TAB, list).set();

		/// alternatives
		BorderPane alternativesTab = new BorderPane();
		TreeTableView<AlternativeTreeItem> alternativesTable = instance.alternativesTable;
		alternativesTab.setCenter(alternativesTable);
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
		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("alternatives"), "alternatives", SetPanel.PanelType.PANEL, alternativesTab).set();

		/// debug
		BorderPane debugTab = new BorderPane();
		TextField debugMsgTag = new TextField("DeskChan:say");
		debugTab.setTop(debugMsgTag);
		TextArea debugMsgData = new TextArea("{\n\"text\": \"Test\"\n}");
		debugTab.setCenter(debugMsgData);

		Button button = new Button(Main.getString("send"));
		button.setOnAction(event -> {
			String tag = debugMsgTag.getText().trim();
			String dataStr = debugMsgData.getText();
			try {
				Main.getPluginProxy().sendMessage(tag, stringToMap(dataStr));
			} catch (Throwable e) {
				Main.log(e);
			}
		});

		Button reloadButton = new Button(Main.getString("reload-style"));
		reloadButton.setOnAction(event -> {
			instance.applyStyle();
			instance.hide();
			instance.show();
		});

		HBox buttons = new HBox(button, reloadButton);
		buttons.setId("controls-bottom-buttons");
		debugTab.setBottom(buttons);
		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("debug"), "debug", SetPanel.PanelType.PANEL, debugTab).set();

	}

	enum CommandsTableType { ACCORDION, TABLE }
	/** Creating 'Commands' tab. **/
	private static void initCommandsTab(CommandsTableType type){
		if (type == CommandsTableType.ACCORDION)
			commandsTable = new CommandsAccordion();
		else
			commandsTable = new CommandsTable();

		BorderPane commandTab = new BorderPane();
		commandTab.setId("commandTab");
		commandTab.setCenter((Node) commandsTable);

		// 'Saved' label
		Text savedText = new Text();
		savedText.setId("saved-label");

		// 'Delete' button
		Button deleteButton = new Button(Main.getString("delete"));
		deleteButton.setId("delete");
		deleteButton.setOnAction(event -> {
			if (commandsTable.getSelected() != null) {
				commandsTable.delete(commandsTable.getSelected());
				savedText.setText(Main.getString("not-saved"));
			}
		});

		// 'Edit' button
		Button editButton = new Button(Main.getString("edit"));
		editButton.setId("edit");
		editButton.setOnAction(event -> {
			if (commandsTable.getSelected() != null) {
				EditCommandDialog dialog = new EditCommandDialog(commandsTable.getSelected());
				savedText.setText(Main.getString("not-saved"));
			}
		});

		// 'Reset' button
		Button resetButton = new Button(Main.getString("reset"));
		resetButton.setId("reset");
		resetButton.setOnAction(event -> {
			CommandsProxy.reset();
			commandsTable.reset();
			savedText.setText("");
		});

		// 'Save' button
		Button saveButton = new Button(Main.getString("save"));
		saveButton.setId("save");
		saveButton.setOnAction(event -> {
			CommandsProxy.setLinks(commandsTable.getLinks());
			CommandsProxy.save();
			savedText.setText("");
		});

		// 'Load' button
		Button loadButton = new Button(Main.getString("load"));
		loadButton.setId("load");
		loadButton.setOnAction(event -> {
			CommandsProxy.load();
			commandsTable.reset();
			savedText.setText("");
		});

		// 'Add' button
		Button addButton = new Button(Main.getString("add"));
		addButton.setId("add");
		addButton.setOnAction(event -> {
			EditCommandDialog dialog = new EditCommandDialog(null);
			savedText.setText(Main.getString("not-saved"));
		});

		// Toggle view
		Button toggleView = new Button();
		if (type == CommandsTableType.ACCORDION)
			toggleView.setText("▦");
		else
			toggleView.setText("▤");
		toggleView.setOnAction(actionEvent -> {
			CommandsTableType oldType, newType;
			try {
				oldType = CommandsTableType.valueOf(Main.getProperties().getString("commands-type", "ACCORDION").toUpperCase());
			} catch (Exception e) {
				oldType = CommandsTableType.ACCORDION;
			}

			if (oldType == CommandsTableType.ACCORDION)
				newType = CommandsTableType.TABLE;
			else
				newType = CommandsTableType.ACCORDION;
			Main.getProperties().put("commands-type", newType.toString());
			initCommandsTab(newType);
		});

		Button help = new Button("?");
		help.setOnAction(actionEvent -> {
			String helpLink = "https://github.com/DeskChan/DeskChan/wiki/%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D1%8F-%D0%B8-%D0%BA%D0%BE%D0%BC%D0%B0%D0%BD%D0%B4%D1%8B";
			try {
				Browser.browse(helpLink);
			} catch (Exception e){
				Main.log(new Exception("Cannot open help link: "+ helpLink, e));
			}
		});

		// Adding buttons to form
		HBox buttons = new HBox(addButton, editButton, deleteButton, loadButton, saveButton, resetButton);
		Region space = new Region();
		HBox.setHgrow(space, Priority.ALWAYS);
		buttons.getChildren().addAll(space, getFlowPane("toggle-view", savedText, toggleView, help));
		commandTab.setBottom(buttons);
		buttons.setId("controls-bottom-buttons");

		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("commands"), "commands", SetPanel.PanelType.TAB, commandTab).set();
	}


	private void initTabs() {
		GridPane gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");

		/// general
		generalOptions();

		/// appearance
		initMainTab();


		/// commands
		CommandsTableType oldType;
		try {
			oldType = CommandsTableType.valueOf(Main.getProperties().getString("commands-type", "ACCORDION").toUpperCase());
		} catch (Exception e) {
			oldType = CommandsTableType.ACCORDION;
		}
		initCommandsTab(oldType);


		/// update alternatives tabls
		Main.getPluginProxy().sendMessage("core:query-alternatives-map", null, (sender, data) -> {
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

		/// plugins
		BorderPane pluginsTab = new BorderPane();
		pluginsTab.setCenter(pluginsList.wrap());
		pluginsList.setId("plugins-list");

		//pluginsList.minWidthProperty().bind(pluginsTab.prefWidthProperty());


		for (String id : PluginManager.getInstance().getPlugins()) {
			pluginsList.add(new PluginListItem(id, false));
		}

		// Blacklisted plugins that not registered in program
		for (String id : PluginManager.getInstance().getBlacklistedPlugins()) {
			pluginsList.add(new PluginListItem(id, true));
		}

		// Load button
		HBox hbox = new HBox();
		hbox.setId("controls-bottom-buttons");
		Button button = new Button(Main.getString("load"));
		button.setOnAction(event -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle(Main.getString("load_plugin"));
			chooser.setInitialDirectory(PluginManager.getPluginsDirPath());
			File file = chooser.showOpenDialog(OptionsDialog.this.getDialogPane().getScene().getWindow());
			if (file != null) {
				try {
					PluginManager.getInstance().loadPluginByPath(new Path(file));
				} catch (Throwable e) {
					Main.log(e);
				}
			}
		});

		PluginOptionsControlItem.HyperlinkItem link = new PluginOptionsControlItem.HyperlinkItem();
		link.init(null, "https://forum.deskchan.info/category/6/plugins");
		link.setText(Main.getString("more")+"...");

		hbox.getChildren().addAll(button, link);



		pluginsTab.setBottom(hbox);
		new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("plugins"), "plugins", SetPanel.PanelType.TAB, pluginsTab).set();

		List<ControlsPanel> tabs = ControlsPanel.getPanels(SetPanel.PanelType.TAB);
		/// Creating top tabs from registered tabs list
		for (ControlsPanel tab : tabs)
			registerTab(tab);

		setPanel(tabs.get(0));
		panelsHistory.add(tabs.get(0));
	}

	static void registerTab(ControlsPanel panel) {
		if (instance == null) return;

		ListView<String> list = instance.tabListView;
		if (list.getItems().contains(panel.name))
			list.getItems().remove(panel.name);
		list.getItems().add(panel.name);
	}

	static void registerSubmenu(ControlsPanel panel) {
		if (instance == null) return;
		for(PluginListItem pli : instance.pluginsList.getItems()){
			if(pli.id.equals(panel.owner)) {
				pli.updateOptionsSubMenu();
				return;
			}
		}
	}

	static void unregisterTab(ControlsPanel panel) {
		if (instance == null) return;
		instance.tabListView.getItems().remove(panel.name);
	}

	static void unregisterSubmenu(ControlsPanel panel) {
		if (instance == null) return;
		for(PluginListItem pli : instance.pluginsList.getItems()){
			if(pli.id.equals(panel.owner)) {
				pli.updateOptionsSubMenu();
				return;
			}
		}
	}
	private static ControlsPanel panelToOpen = null;

	static void showPanel(ControlsPanel panel){
		panelToOpen = panel;
		open();
	}

	private void setPanel(ControlsPanel panel){
		controlsPane.getChildren().clear();
		if (panel == null) return;

		Pane pane = panel.createControlsPane(instance);
		//pane.prefHeightProperty().bind(controlsPane.heightProperty());
		//pane.prefWidthProperty().bind(controlsPane.widthProperty());
		controlsPane.getChildren().add(pane);
	}

	private void appendToHistory(ControlsPanel panel){
		panelIndex++;
		while (panelsHistory.size() > panelIndex) panelsHistory.removeLast();
		panelsHistory.add(panel);
		while (panelsHistory.size() > 15) panelsHistory.removeFirst();
		panelIndex = Math.min(panelIndex, 14);
		updateLinks();
	}

	protected static void open() {
		App.showWaitingAlert(() -> {
			OptionsDialog optionsDialog = OptionsDialog.getInstance();
			if (optionsDialog == null)
				optionsDialog = new OptionsDialog();

			if (!optionsDialog.isShowing()) {
				optionsDialog.show();
			}
			Stage stage = (Stage) optionsDialog.getDialogPane().getScene().getWindow();
			stage.setIconified(false);
			optionsDialog.requestFocus();

			if (panelToOpen != null){
				optionsDialog.setPanel(panelToOpen);
				optionsDialog.appendToHistory(panelToOpen);
				panelToOpen = null;
			}
		});
	}

	// -- Technical classes --

	private static class PluginList extends VBox {

		private List<PluginListItem> items = new ArrayList<>();

		void add(PluginListItem item){
			for (PluginListItem i : items)
				if (i.id.equals(item.id)) return;

			items.add(item);
			getChildren().add(item.getNode());
		}

		void remove(String id){
			for (PluginListItem i : items)
				if (i.id.equals(id)){
					if(!i.blacklisted) {
						items.remove(i);
						getChildren().remove(i.getNode());
					}
					return;
				}
		}

		List<PluginListItem> getItems(){ return items; }

		ScrollPane wrap(){
			ScrollPane pane = new ScrollPane();
			pane.setContent(this);
			pane.setFitToWidth(true);
			return pane;
		}
	}

	/** Class representing row in 'Plugins' tab. **/
	private static class PluginListItem {

		/** List of plugins cannot be simply removed. **/
		private static final String[] importantPlugins = new String[]{
				"core", "core_utils", Main.getPluginProxy().getId(), "talking_system"
		};

		String id;
		String name;
		String link;
		boolean blacklisted;
		VBox vbox = new VBox();
		HBox hbox = new HBox();
		HBox menuBox = new HBox();
		Button blacklistPluginButton;
		Label label;
		Tooltip tooltip;
		Pane pane = new Pane();
		private static final String locked   = "🔒";
		private static final String unlocked = "🔓";

		PluginListItem(String id, boolean blacklisted) {
			this.id = id;
			this.blacklisted = blacklisted;
			setInfo();
		}

		Node getNode(){ return vbox; }

		void setInfo(){

			PluginConfig config = PluginManager.getInstance().getPluginConfig(id);
			if (config != null) {
				name = PluginManager.getInstance().getPluginConfig(id).getName();
				link = (String) PluginManager.getInstance().getPluginConfig(id).get("link");
			} else {
				name = id;
			}

			// Adding char in circle to plugin name if we know its type
			label = new Label(toString());
			label.setAlignment(Pos.CENTER_LEFT);

			Label pluginLetter = new Label(getPluginTypeLetter());
			pluginLetter.getStyleClass().add("plugin-type-letter");

			// Filling row content
			hbox.getChildren().clear();
			hbox.getChildren().addAll(pluginLetter, label, pane, menuBox);

			vbox.getChildren().clear();
			vbox.setAlignment(Pos.CENTER_LEFT);
			vbox.getChildren().add(hbox);

			label.getStyleClass().add("plugin-name");
			hbox.getStyleClass().add("plugin-line");
			menuBox.getStyleClass().add("plugin-menu-box");
			vbox.getStyleClass().add("plugin-cell");
			if (blacklisted)
				vbox.getStyleClass().add("blacklisted");

			// Adding tooltip with plugin information
			try {
				tooltip = new Tooltip(PluginManager.getInstance().getPluginConfig(id).getShortDescription());
			} catch (Exception e){
				tooltip = new Tooltip(Main.getString("no-info"));
			}

			try {
				final String description = config.getDescription();
				if(description != null || link != null) {
					Button infoPluginButton = new Button("?");
					infoPluginButton.setTooltip(new Tooltip(Main.getString("info.plugin-info")));
					if (description != null && link != null){
						infoPluginButton.setOnAction(event -> {
							Label t = new Label(description);
							t.setWrapText(true);
							VBox box = new VBox(t, newHyperlink(Main.getString("documentation"), e -> {
									try {
										Browser.browse(link);
									} catch (Exception ex){
										Main.log(ex);
									}
							}));
							App.showNotification(Main.getString("info"), box);
						});
					} else if (link != null){
						infoPluginButton.setOnAction(event -> {
							try {
								Browser.browse(link);
							} catch (Exception ex){
								Main.log(ex);
							}
						});
					} else {
						infoPluginButton.setOnAction(event -> {
							App.showNotification(Main.getString("info"), description);
						});
					}
					hbox.getChildren().add(infoPluginButton);
				} else if (config.get("link") != null){

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
				System.out.println(id);
				if (!blacklisted)
					for(String pluginId : importantPlugins) {
						if (pluginId.equals(id)) {
							if (alert()) item.toggleBlacklisted();
							return;
						}
					}
				item.toggleBlacklisted();
			});

			hbox.getChildren().addAll(blacklistPluginButton, unloadPluginButton);
			HBox.setHgrow(pane, Priority.ALWAYS);

			updateOptionsSubMenu();

			Tooltip.install(hbox, tooltip);
		}

		/** Update all submenus content. **/
		void updateOptionsSubMenu(){
			List<ControlsPanel> list = ControlsPanel.getPanels(id, SetPanel.PanelType.SUBMENU);
			vbox.getChildren().clear();
			vbox.getChildren().add(hbox);
			if(list == null) return;
			for(ControlsPanel container : list){
				Button button = new Button(container.name);
				button.setOnAction((event) -> {
					container.show();
				});
				vbox.getChildren().add(button);
			}
		}

		/** Toggle blacklisting of plugin. **/
		void toggleBlacklisted(){
			blacklisted = !blacklisted;
			if (blacklisted) {
				PluginManager.getInstance().addPluginToBlacklist(id);
				blacklistPluginButton.setText(locked);
				vbox.getStyleClass().add("blacklisted");
			} else {
				PluginManager.getInstance().removePluginFromBlacklist(id);
				PluginManager.getInstance().tryLoadPluginByName(id);
				blacklistPluginButton.setText(unlocked);
				vbox.getStyleClass().remove("blacklisted");
				setInfo();
			}
			label.setText(toString());
			menuBox.setVisible(!blacklisted);
		}

		@Override
		public String toString() {
			return name + (blacklisted ? (" ["+Main.getString("blacklisted")+"]") : "");
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
		boolean newItem = false;

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

		CommandItem(String event, String command) {
			this.event = event;
			this.command = command;
		}

		CommandItem(String event) {
			this.event = event;
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
			data.put("isDefault", !newItem);

			return data;
		}
	}

	interface CommandsPane {
		void delete(CommandItem item);
		void reset();
		void add(CommandItem item);
		List getLinks();
		CommandItem getSelected();
	}

	static class CommandsTable extends TableView implements CommandsPane {

		CommandsTable() {
			setEditable(true);
			setPlaceholder(new Label(Main.getString("commands.empty")));
			//commandsTable.prefHeightProperty().bind(commandTab.heightProperty());

			// Events column
			TableColumn eventCol = new TableColumn(Main.getString("events"));
			eventCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("event"));
			List events = CommandsProxy.getEventsList();
			Collections.sort(events);
			eventCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(),
					FXCollections.observableArrayList(events)));
			eventCol.setOnEditCommit(ev -> {
				TableColumn.CellEditEvent<CommandItem, String> event = (TableColumn.CellEditEvent<CommandItem, String>) ev;
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
				TableColumn.CellEditEvent<CommandItem, String> event = (TableColumn.CellEditEvent<CommandItem, String>) ev;
				CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
				item.setCommand(event.getNewValue());
			});
			commandCol.setMinWidth(120);

			// Rules column
			TableColumn ruleCol = new TableColumn(Main.getString("rules"));
			ruleCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("rule"));
			ruleCol.setCellFactory(TooltippedTableCell.<CommandItem>forTableColumn());
			ruleCol.setOnEditCommit(ev -> {
				TableColumn.CellEditEvent<CommandItem, String> event = (TableColumn.CellEditEvent<CommandItem, String>) ev;
				CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
				item.setRule(event.getNewValue());
			});
			ruleCol.setMinWidth(120);

			// Parameters column
			TableColumn msgCol = new TableColumn(Main.getString("parameters"));
			msgCol.setCellValueFactory(new PropertyValueFactory<CommandItem, String>("msgData"));
			msgCol.setCellFactory(TooltippedTableCell.<CommandItem>forTableColumn());
			msgCol.setOnEditCommit(ev -> {
				TableColumn.CellEditEvent<CommandItem, String> event = (TableColumn.CellEditEvent<CommandItem, String>) ev;
				CommandItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
				item.setMsgData(event.getNewValue());
			});
			msgCol.setMinWidth(120);

			reset();

			// Setting columns
			getColumns().addAll(eventCol, ruleCol, commandCol, msgCol);
		}

		public void reset(){

			// Filling list of commands
			ObservableList<CommandItem> list = FXCollections.observableArrayList();
			for(Map<String,Object> entry : CommandsProxy.getLinksList()){
				list.add(new CommandItem(entry));
			}

			setItems(list);
		}

		public void add(CommandItem item){
			getItems().add(item);
		}

		public CommandItem getSelected(){
			if(getSelectionModel().getSelectedIndex() >= 0)
				return (CommandItem) getSelectionModel().getSelectedItem();
			return null;
		}

		public void delete(CommandItem item){
			getItems().remove(item);
		}

		public List getLinks(){
			ArrayList<Map<String,Object>> push = new ArrayList<>();
			for(CommandItem item : (ObservableList<CommandItem>) getItems()){
				push.add(item.toMap());
			}

			return push;
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

	}

	static class CommandsAccordion extends Accordion implements CommandsPane {

		Property<CommandBox> selected = new SimpleObjectProperty<>(null);
		Map <String, CommandTitledPane> commands;
		CommandsAccordion(){
			reset();
		}

		static class CommandTitledPane extends TitledPane {
			CommandTitledPane(String title, ListView content, Property<CommandBox> property){
				super(title, content);
				expandedProperty().addListener((observableValue, aBoolean, t1) -> {
					property.setValue(null);
				});
				content.getSelectionModel().selectedItemProperty().addListener((observableValue, old, commandBox) -> {
					property.setValue((CommandBox) commandBox);
				});
				content.setPlaceholder(new Text(Main.getString("empty")));
				setTooltip(new Tooltip(EditCommandDialog.events.getOrDefault(title, Main.getString("no-info"))));
			}
			ListView<CommandBox> getList(){
				return (ListView<CommandBox>) getContent();
			}
		}

		public CommandBox toCell(CommandItem item){
			CommandBox box = new CommandBox(item);
			box.getStyleClass().add("command-link");

			Text c = new Text(item.command);
			c.getStyleClass().add("command");
			Text ruleField = new Text(item.rule);
			ruleField.getStyleClass().add("rule");

			Region hgap = new Region();
			box.setHgrow(hgap, Priority.ALWAYS);

			box.getChildren().addAll(c, hgap, ruleField);

			return box;
		}

		public void reset(){

			getPanes().clear();

			selected.setValue(null);
			commands = new HashMap<>();

			for(Map<String,Object> entry : CommandsProxy.getLinksList()){
				CommandItem item = new CommandItem(entry);
				add(item);
			}

			for (String event : CommandsProxy.getEventsList()){
				if (commands.containsKey(event)) continue;

				CommandTitledPane pane = new CommandTitledPane(event, new ListView<>(), selected);
				commands.put(event, pane);
			}

			getPanes().addAll(commands.values());
		}

		public void add(CommandItem item){
			ListView<CommandBox> list;
			if (!commands.containsKey(item.event)) {
				list = new ListView<>();
				CommandTitledPane pane = new CommandTitledPane(item.event, list, selected);
				commands.put(item.event, pane);
			} else {
				list = commands.get(item.event).getList();
			}
			list.getItems().add(toCell(item));
		}

		public CommandItem getSelected(){
			return selected.getValue() != null ? selected.getValue().item : null;
		}

		public void delete(CommandItem item){
			ListView<CommandBox> list = commands.get(item.event).getList();
			if (selected.getValue().item == item)
				selected.setValue(null);
			list.getItems().removeIf(commandBox -> commandBox.item == item);
		}


		public List getLinks(){
			ArrayList<Map<String,Object>> push = new ArrayList<>();
			for (CommandTitledPane pane : commands.values()) {
				for (CommandBox item : pane.getList().getItems()) {
					push.add(item.item.toMap());
				}
			}

			return push;
		}

		static class CommandBox extends HBox {
			CommandItem item;
			CommandBox(CommandItem item){ this.item = item; }
		}
	}

	static class EditCommandDialog extends TemplateBox {

		private static EditCommandDialog instance;
		private ComboBox<String> event = new ComboBox<>();
		private Label eventInfo = new Label();

		private ComboBox<String> command = new ComboBox<>();
		private Label commandInfo = new Label();

		private TextField rule = new TextField();
		private Label ruleInfo = new Label();

		private GridPane msgGrid = new GridPane();
		private Map<String, TextField> msgElements = new HashMap<>();

		static { reloadInfo(); }

		EditCommandDialog(CommandItem item){
			super("edit-command", Main.getString("edit-command"));
			if (instance != null && instance != this)
				instance.close();

			setResizable(true);
			instance = this;

			VBox content = new VBox();
			content.getChildren().addAll(
					event, eventInfo, new Separator(),
					command, commandInfo, new Separator(),
					ruleInfo, rule
			);
			content.setId("content");
			getDialogPane().setContent(new Group(content));


			event.setCellFactory(stringListView ->
				new ListCell<String>() {
					@Override protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						setText(item);
						if (!eventsKeys.contains(item) && !commands.containsKey(item)){
							Label l = new Label("X");
							l.setId("non-exist");
							setGraphic(l);
						} else {
							setGraphic(null);
						}
					}
				}
			);

			command.setCellFactory(stringListView ->
					new ListCell<String>() {
						@Override protected void updateItem(String item, boolean empty) {
							super.updateItem(item, empty);
							setText(item);
							if (!eventsKeys.contains(item) && !commandsKeys.contains(item)){
								Label l = new Label("X");
								l.setId("non-exist");
								setGraphic(l);
							} else {
								setGraphic(null);
							}
						}
					}
			);
			eventInfo.setWrapText(true);
			commandInfo.setWrapText(true);
			ruleInfo.setWrapText(true);

			for (String eventName : eventsKeys)
				event.getItems().add(eventName);
			for (String commandName : commandsKeys)
				command.getItems().add(commandName);

			event.getSelectionModel().selectedItemProperty().addListener((obs, old, text) -> {
				eventInfo.setText(events.getOrDefault(text, Main.getString("no-info")));
				if (!rules.containsKey(text)) {
					rule.setDisable(true);
					ruleInfo.setText(Main.getString("no-rule-required"));
				} else {
					rule.setDisable(false);
					ruleInfo.setText(rules.get(text));
				}
			});

			command.getSelectionModel().selectedItemProperty().addListener((obs, old, text) -> {
				commandInfo.setText(commands.getOrDefault(text, Main.getString("no-info")));
				Object m = msgs.get(text);
				content.getChildren().remove(msgGrid);

				msgElements = new HashMap<>();

				if (m != null){
					msgGrid = new GridPane();
					if (m instanceof Map){
						int index = 0;
						Map itemData = item != null && item.msgData instanceof Map ? (Map) item.msgData : null;
						String itemDataString = item != null && item.msgData != null ? item.msgData.toString() : null;
						for (Map.Entry<String, Object> entry : ((Map<String, Object>) m).entrySet()){
							msgGrid.add(new Text(entry.getKey()), 0, index);
							String str = (itemData != null && itemData.get(entry.getKey()) != null) ? itemData.get(entry.getKey()).toString() : (index == 0 && itemDataString != null ? itemDataString : "");
							TextField t = new TextField(str);
							msgGrid.add(t, 1, index);
							msgGrid.add(new ControlsPanel.Hint(entry.getValue().toString()), 2, index);
							msgElements.put(entry.getKey(), t);
							index++;
						}
					} else {
						msgGrid.add(new Text(Main.getString("message")), 0, 0);
						TextField t = new TextField(item != null && item.msgData != null ? item.msgData.toString() : "");
						msgGrid.add(t, 1, 0);
						msgGrid.add(new ControlsPanel.Hint(m.toString()), 2, 0);
						msgElements.put("value", t);
					}
					content.getChildren().add(msgGrid);
				} else {
					msgGrid.add(new Text(Main.getString("message")), 0, 0);
					TextField t = new TextField(item != null && item.msgData != null ? item.msgData.toString() : "");
					t.setDisable(true);
					msgGrid.add(t, 1, 0);
					msgElements.put("value", t);
				}
				content.applyCss();
				content.layout();
				setHeight(content.getBoundsInParent().getHeight() + 8 * App.getInterfaceScale());
			});

			getDialogPane().getButtonTypes().clear();
			ButtonType cancel = new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE),
					   save   = new ButtonType(Main.getString("save"));
			getDialogPane().getButtonTypes().add(cancel);
			getDialogPane().getButtonTypes().add(save);
			getDialogPane().lookupButton(save).addEventHandler(ActionEvent.ACTION, actionEvent -> {
				if (openedItem != null)
					commandsTable.delete(openedItem);

				commandsTable.add(getResultItem());
				instance = null;
			});
			addOnCloseRequest(dialogEvent -> {
				hide();
				instance = null;
			});

			open(item);
		}

		static Map<String, String> events;
		static List<String> eventsKeys;
		static Map<String, String> commands;
		static List<String> commandsKeys;
		static Map<String, String> rules;
		static Map<String, Object> msgs;

		static void reloadInfo(){
			events = new HashMap<>();
			commands = new HashMap<>();
			rules = new HashMap<>();
			msgs = new HashMap<>();
			eventsKeys = new LinkedList<>(events.keySet());
			commandsKeys = new LinkedList<>(commands.keySet());

			for (String event : CommandsProxy.getEventsList()){
				Map<String, Object> eventInfo = CommandsProxy.getEventInfo(event);
				eventsKeys.add(event);
				if (eventInfo.get("info") != null) {
					events.put(event, eventInfo.get("info").toString());
				}
				if (eventInfo.get("ruleInfo") != null)
					rules.put(event, eventInfo.get("ruleInfo").toString());
			}
			for (String command : CommandsProxy.getCommandsList()){
				Map<String, Object> commandInfo = CommandsProxy.getCommandInfo(command);
				commandsKeys.add(command);
				if (commandInfo.get("info") != null) {
					commands.put(command, commandInfo.get("info").toString());
				}
				if (commandInfo.get("msgInfo") != null){
					msgs.put(command, commandInfo.get("msgInfo"));

				}
			}

			Collections.sort(eventsKeys);
			Collections.sort(commandsKeys);

			if (instance != null)
				instance.open(instance.openedItem);
		}

		private CommandItem openedItem;
		void open(CommandItem item){

			if (item != null){
				if (!events.containsKey(item.event)) {
					event.getItems().add(0, item.event);
				}

				if (!commands.containsKey(item.command)) {
					command.getItems().add(0, item.command);
				}

				rule.setText(item.rule);

				event.getSelectionModel().select(item.event);
				command.getSelectionModel().select(item.command);
			} else {
				event.getSelectionModel().select(0);
				command.getSelectionModel().select(0);
			}


			openedItem = item;
			show();
			requestFocus();
		}

		CommandItem getResultItem(){
			CommandItem result = new CommandItem(
					event.getSelectionModel().getSelectedItem(),
				  command.getSelectionModel().getSelectedItem()
			);
			result.rule = rule.getText();

			Map<String, String> m = new HashMap<>();
			for (Map.Entry<String, TextField> entry : msgElements.entrySet()){
				if (entry.getValue().getText().length() > 0)
					m.put(entry.getKey(), entry.getValue().getText());
			}
			Object msgData = m;
			if (msgElements.size() == 1 && msgElements.get("value") != null)
				msgData = msgElements.get("value").getText();
			result.msgData = msgData;

			result.newItem = true;

			return result;
		}
	}


	private static Map stringToMap(String text) throws Exception{
		text = text.trim();
		if (!text.startsWith("{")) text = "{" + text;
		if (!text.endsWith("}"))   text = text + "}";

		JSONObject json = new JSONObject(text);
		return json.toMap();
	}

	private static FlowPane getFlowPane(String id, Node... items){
		FlowPane pane = new FlowPane(items);
		pane.setId(id);
		return pane;
	}

	private static Hyperlink newHyperlink(String text, EventHandler value){
		Hyperlink link = new Hyperlink(text);
		link.setOnAction(value);
		return link;
	}
}