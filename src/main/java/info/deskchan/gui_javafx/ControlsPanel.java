package info.deskchan.gui_javafx;

import info.deskchan.MessageData.GUI.SetPanel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.rmi.NoSuchObjectException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ControlsPanel {

	private static Map<String, ControlsPanel> registeredPanels = new HashMap<>();
	private static ListView<String> registeredPanelsListView = new ListView<>();

	static {
		try {
			new ControlsPanel(Main.getPluginProxy().getId(), Main.getString("panels-id"), "panels-id", SetPanel.PanelType.SUBMENU, new Pane(registeredPanelsListView)).set();
			registeredPanelsListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent click) {
					if (click.getClickCount() == 2) {
						String item = registeredPanelsListView.getSelectionModel().getSelectedItem();
						item = item.substring(1 + item.lastIndexOf('('), item.length() - 1);
						open(item);
					}
				}
			});
		} catch (Exception e){
			Main.log(e);
		}
	}

	final String name;
	final String id;
	Region panelPane;
	BorderPane wrapper;
	List<Map<String, Object>> controls;
	String msgSave;
	String msgClose;
	String owner;
	TemplateBox parentWindow;
	boolean predefinedPane = false;
	
	SetPanel.PanelType type;

	private Map<String, PluginOptionsControlItem> namedControls;

	private static float columnGrow = 0.5f;

	ControlsPanel(String sender, String name, String id) {
		this.name = name;
		this.owner = sender;
		this.id = id;
	}

	ControlsPanel(String sender, String name, String id, SetPanel.PanelType type, Pane panel) {
		this.name = name;
		this.owner = sender;
		this.id = id;
		this.panelPane = panel;
		this.type = type;
		predefinedPane = true;
	}

	ControlsPanel(String sender, String type, String id, Map<String, Object> data) {
		this (
				sender,
				(String) data.get("name"),
				type,
				id,
				(List<Map<String, Object>>) data.getOrDefault("controls", new LinkedList<Map>()),
				getMsgTag(data),
				(String) data.get("onClose"),
				(String) data.get("action")
		);
	}

	ControlsPanel(String sender, String name, String id, SetPanel.PanelType type, List<Map<String, Object>> controls) {
		this (
				sender,
				name,
				id,
				type.toString(),
				controls,
				null,
				null,
				null
		);
	}

	ControlsPanel(String sender, Map<String, Object> data) {
		this (
				sender,
				(String) data.get("name"),
				(String) data.get("id"),
				data.getOrDefault("type", "tab").toString(),
				(List<Map<String, Object>>) data.get("controls"),
				getMsgTag(data),
				(String) data.get("onClose"),
				(String) data.get("action")
		);
	}

	ControlsPanel(String sender, String name, String id, String type, List<Map<String, Object>> controls, String msgSave, String msgClose, String action) {
		this.type = getType(type);

		if (name == null) {
			switch (this.type) {
				case SUBMENU:
					name = Main.getString("options");
					break;
				case TAB:
					name = sender;
					break;
				default:
					name = Main.getString("default_messagebox_name");
					break;
			}
		}
		this.name = name;
		this.id = id;
		this.controls = controls;
		this.msgSave = msgSave;
		this.msgClose = msgClose;
		this.owner = sender;

		try {
			SetPanel.ActionType actionType = SetPanel.ActionType.valueOf(action.toUpperCase());
			Platform.runLater(() -> {
				switch (actionType){
					case SHOW:   show();   break;
					case HIDE:   hide();   break;
					case SET:    set();    break;
					case UPDATE: update(); break;
					case DELETE: delete(); break;
				}
			});
		} catch (Exception e){
			set();
		}

	}

	String getFullName(){
		return owner + "-" + (id != null ? id : name);
	}

	public void set(){
		ControlsPanel oldPanel = registeredPanels.put(getFullName(), this);
		registeredPanelsListView.getItems().add(name + " (" + getFullName() + ")");
		if (oldPanel != null){
			if (type == null) type = oldPanel.type;
			if (oldPanel.parentWindow != null && oldPanel.parentWindow.getDialogPane() != null && oldPanel.parentWindow.getDialogPane().getScene() != null) {
				wrapper = oldPanel.wrapper;
				Platform.runLater(() -> {
					createControlsPane(oldPanel.parentWindow);
				});
			}
		} else {
			switch (type) {
				case TAB: {
					OptionsDialog.registerTab(this);
				}
				break;
				case SUBMENU: {
					OptionsDialog.registerSubmenu(this);
				}
				break;
			}
		}
	}

	public void update(){
		ControlsPanel currentPanel = registeredPanels.get(getFullName());
		if (currentPanel == null)
			set();
		else
			currentPanel.updateControlsPane(controls);
	}

	public void show(){
		ControlsPanel _currentPanel = registeredPanels.get(getFullName());
		if (_currentPanel == null || controls != null || panelPane != null) {
			set();
			_currentPanel = this;
		}
		final ControlsPanel currentPanel = _currentPanel;
		switch (currentPanel.type) {
			case WINDOW: {
				ControlsWindow.open(currentPanel);
			}
			break;
			default: {
				OptionsDialog.showPanel(currentPanel);
			}
			break;
		}
	}

	public void hide(){
		ControlsPanel currentPanel = registeredPanels.get(getFullName());
		if (currentPanel == null) {
			set();
			currentPanel = this;
		}

		switch (currentPanel.type){
			case WINDOW:{
				Platform.runLater(() -> ControlsWindow.closeCustomWindow(this));
			} break;
			case TAB: case SUBMENU: case PANEL:{
				OptionsDialog.showPanel(null);
			} break;
		}
	}

	public static void open(String panelName){
		ControlsPanel panel = registeredPanels.get(panelName);
		if (panel == null) {
			Main.log(new NoSuchObjectException("Unknown panel by name: " + panelName));
			return;
		}

		panel.show();
	}

	public void delete(){
		hide();
		switch (type){
			case TAB:{
				OptionsDialog.unregisterTab(this);
			} break;
			case SUBMENU:{
				OptionsDialog.unregisterSubmenu(this);
			} break;
		}
		final String n = getFullName();
		registeredPanels.remove(n);
		registeredPanelsListView.getItems().removeIf(s -> s.contains(n));
	}

	private String getSaveTag(){
		return msgSave;
	}

	private String getCloseTag(){
		return msgClose;
	}

	Pane createControlsPane(TemplateBox parent) {

		if (!predefinedPane && (panelPane == null || parent != parentWindow)){
			GridPane gridPane = new GridPane();
			gridPane.getStyleClass().add("grid-pane");

			float columnGrowPercentage = columnGrow * 100;

			ColumnConstraints column1 = new ColumnConstraints();
			//column1.setPercentWidth(columnGrowPercentage);

			ColumnConstraints column2 = new ColumnConstraints();
			//column2.setPercentWidth(90 - columnGrowPercentage);

			ColumnConstraints column3 = new ColumnConstraints();
			//column3.setPercentWidth(5);

			gridPane.getColumnConstraints().addAll(column1, column2);

			namedControls = new HashMap<>();

			int row = 0;
			for (Map<String, Object> controlInfo : controls) {
				String label = (String) controlInfo.get("label");
				String hint = (String) controlInfo.get("hint");

				Node node;
				if (controlInfo.containsKey("elements")) {
					HBox box = new HBox();
					node = box;
					box.setId((String) controlInfo.get("id"));
					for (Map element : (List<Map>) controlInfo.get("elements")) {
						PluginOptionsControlItem item = initItem(element, parent.getDialogPane().getScene().getWindow());
						if (item != null) box.getChildren().add(item.getNode());
					}
				} else {
					PluginOptionsControlItem item = initItem(controlInfo, parent.getDialogPane().getScene().getWindow());
					if (item != null)
						node = item.getNode();
					else continue;
				}
				if (label == null) {
					gridPane.add(node, 0, row, 2, 1);
				} else {
					Label labelNode = new Label(label + ":");
					labelNode.setFont(LocalFont.defaultFont);
					gridPane.add(labelNode, 0, row);
					gridPane.add(node, 1, row);
				}
				if (hint != null) {
					if (column3 != null) {
						gridPane.getColumnConstraints().add(column3);
						column3 = null;
					}
					gridPane.add(new Hint(hint), 2, row);
				}
				row++;
			}
			panelPane = gridPane;
		}

		parentWindow = parent;

		wrap();

		if (getSaveTag() != null) {
			Button saveButton = new Button(Main.getString("save"));
			saveButton.setOnAction(event -> {
				App.showWaitingAlert(() -> {
					Map<String, Object> data = new HashMap<>();
					for (Map.Entry<String, PluginOptionsControlItem> entry : namedControls.entrySet()) {
						data.put(entry.getKey(), entry.getValue().getValue());
						for (Map<String, Object> control : controls) {
							String id = (String) control.get("id");
							if (id != null && id.equals(entry.getKey())) {
								control.put("value", entry.getValue().getValue());
								break;
							}
						}
					}
					Main.getPluginProxy().sendMessage(getSaveTag(), data);
				});
			});
			HBox box = new HBox(saveButton);
			box.setId("controls-bottom-buttons");
			wrapper.setBottom(box);
		}
		if (getCloseTag() != null) {
			parent.addOnCloseRequest(event -> {
				App.showWaitingAlert(() -> {
					Main.getPluginProxy().sendMessage(getCloseTag(), null);
				});
			});
		}

		return wrapper;
	}

	void wrap(){
		if (wrapper == null) {
			wrapper = new BorderPane();
			wrapper.setId(getFullName());
		} else {
			wrapper.getChildren().clear();
		}

		ScrollPane nodeScrollPanel = new ScrollPane();
		nodeScrollPanel.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		nodeScrollPanel.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		nodeScrollPanel.setFitToHeight(true);
		nodeScrollPanel.setFitToWidth(true);
		//nodeScrollPanel.minWidthProperty().bind(panelPane.widthProperty());
		//nodeScrollPanel.setStyle("-fx-background-color:transparent;");
		nodeScrollPanel.setContent(panelPane);

		wrapper.setCenter(nodeScrollPanel);
	}

	PluginOptionsControlItem initItem(Map controlInfo, Window window){
		PluginOptionsControlItem item =
				PluginOptionsControlItem.create(window, controlInfo);
		if (item == null) return item;
		String id = (String) controlInfo.get("id");
		if (id != null) {
			namedControls.put(id, item);
			item.getNode().setId(id);
			if (item.getProperty() != null) {
				item.getProperty().addListener(new ChangeListener() {
					@Override
					public void changed(ObservableValue observable, Object oldValue, Object newValue) {
						controlInfo.put("value", newValue);
					}
				});
			}
		}
		return item;
	}

	void updateControlsPane(List<Map<String, Object>> update) {
		if (namedControls == null){
			for (Map<String, Object> controlInfo : controls) {
				if (controlInfo.containsKey("elements")) {
					for (Map element : (List<Map>) controlInfo.get("elements")) {
						for (Map<String, Object> control : update) {
							if (control.get("id").equals(element.get("id"))){
								element.putAll(control);
							}
						}
					}
				}
				for (Map<String, Object> control : update) {
					if (control.get("id").equals(controlInfo.get("id"))) {
						controlInfo.putAll(control);
					}
				}
			}
		} else {
			for (Map<String, Object> control : update) {
				String id = (String) control.get("id");
				if (id == null) continue;

				PluginOptionsControlItem item = namedControls.get(id);
				if (item == null) continue;

				Object value = control.get("value");
				if (value != null) namedControls.get(id).setValue(value);

				Boolean disabled = App.getBoolean(control.get("disabled"), null);
				if (disabled != null)
					namedControls.get(id).getNode().setDisable(disabled);
			}
		}
	}

	static class Hint extends Button{

		Hint(String text){
			this(text, "?");
		}
		Hint(String text, String buttonText){
			setText(buttonText);
			getStyleClass().setAll("hint");
			Tooltip tooltip = new Tooltip(text);
			tooltip.setAutoHide(true);
			setTooltip(tooltip);
			setOnMouseClicked(event -> {
				Point2D p = localToScene(0.0, 0.0);
				getTooltip().show(this, p.getX()
						+ getScene().getX() + getScene().getWindow().getX(), p.getY()
						+ getScene().getY() + getScene().getWindow().getY());
			});
		}
	}

	static List<ControlsPanel> getPanels(SetPanel.PanelType type){
		return getPanels(null, type);
	}

	static List<ControlsPanel> getPanels(String owner){
		return getPanels(owner, null);
	}

	static List<ControlsPanel> getPanels(String owner, SetPanel.PanelType panelType){

		List<ControlsPanel> result = new LinkedList<>();
		for (ControlsPanel panel : registeredPanels.values())
			if ((     owner == null || panel.owner.equals(owner) ) &&
				( panelType == null || panel.type == panelType ) )
				result.add(panel);

		return result;
	}

	private static SetPanel.PanelType getType(String type){
		try {
			return SetPanel.PanelType.valueOf(type.toUpperCase());
		} catch (Exception e){
			throw new RuntimeException("Cannot cast " + type + " to SetPanel.PanelType", e.getCause());
		}
	}

	private static String getMsgTag(Map<String, Object> data){
		if (data.get("msgTag") != null) return (String) data.get("msgTag");
		return (String) data.get("onSave");
	}
}
