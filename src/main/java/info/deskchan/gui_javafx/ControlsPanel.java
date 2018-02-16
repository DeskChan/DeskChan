package info.deskchan.gui_javafx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ControlsPanel {

	private static Map<String, ControlsPanel> registeredPanels = new HashMap<>();

	final String name;
	private Pane panelPane;
	List<Map<String, Object>> controls;
	String msgSave;
	String msgClose;
	String owner;

	enum PanelType { TAB, SUBMENU, WINDOW, PANEL, INFO }
	PanelType type;

	private Map<String, PluginOptionsControlItem> namedControls;

	private static float columnGrow = 0.5f;

	ControlsPanel(String sender, String name) {
		this.name = name;
		this.owner = sender;
	}

	ControlsPanel(String sender, String name, PanelType type, Pane panel) {
		this.name = name;
		this.owner = sender;
		this.panelPane = panel;
		this.type = type;
	}

	ControlsPanel(String sender, String type, Map<String, Object> data) {
		this (
				sender,
				data.getOrDefault("name", Main.getString("default_messagebox_name")).toString(),
				type,
			   (List<Map<String, Object>>) data.getOrDefault("controls", new LinkedList<Map>()),
			   (String) data.get("msgTag"),
			   (String) data.get("onClose"),
			   (String) data.get("action")
		);
	}

	ControlsPanel(String sender, String name, PanelType type, List<Map<String, Object>> controls) {
		this (
				sender,
				name,
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
			    data.getOrDefault("name", Main.getString("default_messagebox_name")).toString(),
		        data.getOrDefault("type", "tab").toString(),
		       (List<Map<String, Object>>) data.getOrDefault("controls", new LinkedList<Map>()),
		       (String) data.get("msgTag"),
		       (String) data.get("onClose"),
			   (String) data.get("action")
		);
	}

	ControlsPanel(String sender, String name, String type, List<Map<String, Object>> controls, String msgSave, String msgClose, String action) {
		this.name = name;
		this.controls = controls;
		this.msgSave  = msgSave;
		this.msgClose = msgClose;
		this.owner = sender;
		this.type = getType(type);
		if (action == null) return;
		switch (action.toLowerCase()){
			case "show": show();
			case "hide": hide();
			case "create": create();
			case "update": update();
		}
	}

	public String getFullName(){
		return owner + ":" + name;
	}

	public void create(){
		ControlsPanel oldPanel = registeredPanels.put(getFullName(), this);
		if (oldPanel != null){
			oldPanel.delete();
			if (type == null) type = oldPanel.type;
		}
		switch (type){
			case TAB:{
				OptionsDialog.registerTab(this);
			} break;
			case SUBMENU:{
				OptionsDialog.registerSubmenu(this);
			} break;
		}
	}

	public void update(){
		ControlsPanel currentPanel = registeredPanels.get(getFullName());
		if (currentPanel == null)
			create();
		else
			currentPanel.updateControlsPane(controls);
	}

	public void show(){
		ControlsPanel currentPanel = registeredPanels.get(getFullName());
		if (currentPanel == null) {
			create();
			currentPanel = this;
		}

		switch (currentPanel.type){
			case WINDOW: case INFO:{
				ControlsWindow.setupCustomWindow(this);
			} break;
			default:{
				OptionsDialog.showPanel(this);
			} break;
		}
	}

	public void hide(){
		ControlsPanel currentPanel = registeredPanels.get(getFullName());
		if (currentPanel == null) {
			create();
			currentPanel = this;
		}

		switch (currentPanel.type){
			case WINDOW: case INFO:{
				ControlsWindow.closeCustomWindow(this);
			} break;
		}
	}

	public static void open(String panelName){
		ControlsPanel panel = registeredPanels.get(panelName);
		if (panel == null) {
			System.out.println(registeredPanels.keySet());
			Main.log("Unknown panel by name: " + panelName);
			return;
		}

		panel.show();
	}

	public void delete(){
		switch (type){
			case TAB:{
				OptionsDialog.unregisterTab(this);
			} break;
			case SUBMENU:{
				OptionsDialog.unregisterSubmenu(this);
			} break;
		}
	}

	private String getSaveTag(){
		return msgSave;
	}

	private String getCloseTag(){
		return msgClose;
	}

	Pane createControlsPane(TemplateBox parent) {
		if (panelPane != null)
			return panelPane;

		GridPane gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");

		float columnGrowPercentage = columnGrow * 100;

		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(columnGrowPercentage);

		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(90 - columnGrowPercentage);

		ColumnConstraints column3 = new ColumnConstraints();
		column3.setPercentWidth(5);

		gridPane.getColumnConstraints().addAll(column1, column2, column3);

		namedControls = new HashMap<>();
		BorderPane borderPane = new BorderPane();

		int row = 0;
		for (Map<String, Object> controlInfo : controls) {
			String label = (String) controlInfo.get("label");
			String hint = (String) controlInfo.get("hint");

			Node node;
			if (controlInfo.containsKey("elements")){
				HBox box = new HBox();
				node = box;
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
				Text labelNode = new Text(label + ":");
				labelNode.setFont(LocalFont.defaultFont);
				labelNode.setWrappingWidth(250 * App.getInterfaceMultiplierSize());
				gridPane.add(labelNode, 0, row);
				gridPane.add(node, 1, row);
			}
			if(hint != null){
				gridPane.add(new Hint(hint),2,row);
			}
			row++;
		}
		if (getSaveTag() != null) {
			Button saveButton = new Button(Main.getString("save"));
			saveButton.setOnAction(event -> {
				Map<String, Object> data = new HashMap<>();
				for (Map.Entry<String, PluginOptionsControlItem> entry : namedControls.entrySet()) {
					data.put(entry.getKey(), entry.getValue().getValue());
					for (Map<String, Object> control : controls) {
						String id = (String) control.get("id");
						if (id != null) {
							if (id.equals(entry.getKey())) {
								control.put("value", entry.getValue().getValue());
								break;
							}
						}
					}
				}
				Main.getPluginProxy().sendMessage(getSaveTag(), data);
			});
			borderPane.setBottom(saveButton);
		}
		if (getCloseTag() != null) {
			parent.addOnCloseRequest(event -> {
				Main.getPluginProxy().sendMessage(getCloseTag(), null);
			});
		}
		ScrollPane nodeScrollPanel = new ScrollPane();
		nodeScrollPanel.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		nodeScrollPanel.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		nodeScrollPanel.setFitToHeight(true);
		nodeScrollPanel.setFitToWidth(true);
		nodeScrollPanel.setStyle("-fx-background-color:transparent;");
		nodeScrollPanel.setContent(gridPane);
		nodeScrollPanel.maxHeightProperty().bind(borderPane.prefHeightProperty());

		borderPane.setTop(nodeScrollPanel);
		return borderPane;
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
		for (Map<String, Object> control : update) {
			String id = (String) control.get("id");
			Object value = control.get("value");
			if(value != null) namedControls.get(id).setValue(value);
			Boolean disabled = App.getBoolean(control.get("disabled"), null);
			if(disabled != null)
				namedControls.get(id).getNode().setDisable(disabled);
		}
	}

	class Hint extends Button{
		Hint(String text){
			setText("?");
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

	static List<ControlsPanel> getPanels(PanelType type){
		return getPanels(null, type);
	}

	static List<ControlsPanel> getPanels(String owner){
		return getPanels(owner, null);
	}

	static List<ControlsPanel> getPanels(String owner, PanelType panelType){

		List<ControlsPanel> result = new LinkedList<>();
		for (Map.Entry<String, ControlsPanel> panel : registeredPanels.entrySet())
			if ((     owner == null || panel.getValue().owner.equals(owner) ) &&
				( panelType == null || panel.getValue().type == panelType ) )
				result.add(panel.getValue());

		return result;
	}

	private static PanelType getType(String type){
		try {
			return PanelType.valueOf(type.toUpperCase());
		} catch (Exception e){
			throw new RuntimeException(e.getCause());
		}
	}
}
