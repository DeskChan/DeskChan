package info.deskchan.gui_javafx;

import javafx.application.Platform;
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

import java.rmi.NoSuchObjectException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ControlsPanel {

	private static Map<String, ControlsPanel> registeredPanels = new HashMap<>();

	final String name;
	Region panelPane;
	BorderPane wrapper;
	List<Map<String, Object>> controls;
	String msgSave;
	String msgClose;
	String owner;
	TemplateBox parentWindow;

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
			   (String) data.get("name"),
				type,
			   (List<Map<String, Object>>) data.getOrDefault("controls", new LinkedList<Map>()),
			    getMsgTag(data),
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
			   (String) data.get("name"),
		        data.getOrDefault("type", "tab").toString(),
		       (List<Map<String, Object>>) data.getOrDefault("controls", new LinkedList<Map>()),
				getMsgTag(data),
		       (String) data.get("onClose"),
			   (String) data.get("action")
		);
	}

	ControlsPanel(String sender, String name, String type, List<Map<String, Object>> controls, String msgSave, String msgClose, String action) {
		this.type = getType(type);

		if (name == null){
			switch (this.type){
				case SUBMENU: name = Main.getString("options"); break;
				case TAB: name = sender; break;
				default: name = Main.getString("default_messagebox_name"); break;
			}
		}
		this.name = name;
		this.controls = controls;
		this.msgSave  = msgSave;
		this.msgClose = msgClose;
		this.owner = sender;

		if (action == null) return;
		switch (action.toLowerCase()){
			case "show":   show();   break;
			case "hide":   hide();   break;
			case "set":    set();    break;
			case "update": update(); break;
			case "delete": delete(); break;
		}
	}

	public String getFullName(){
		return owner + ":" + name;
	}

	public void set(){
		ControlsPanel oldPanel = registeredPanels.put(getFullName(), this);
		if (oldPanel != null){
			if (type == null) type = oldPanel.type;
			if (oldPanel.parentWindow != null && oldPanel.parentWindow.getDialogPane() != null && oldPanel.parentWindow.getDialogPane().getScene() != null) {
				wrapper = oldPanel.wrapper;
				createControlsPane(oldPanel.parentWindow);
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
		App.showWaitingAlert(() -> {
			switch (currentPanel.type) {
				case WINDOW:
				case INFO: {
					Platform.runLater(() -> new ControlsWindow(currentPanel));
				}
				break;
				default: {
					OptionsDialog.showPanel(currentPanel);
				}
				break;
			}
		});
	}

	public void hide(){
		ControlsPanel currentPanel = registeredPanels.get(getFullName());
		if (currentPanel == null) {
			set();
			currentPanel = this;
		}

		switch (currentPanel.type){
			case WINDOW: case INFO:{
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
	}

	private String getSaveTag(){
		return msgSave;
	}

	private String getCloseTag(){
		return msgClose;
	}

	Pane createControlsPane(TemplateBox parent) {
		parentWindow = parent;

		if (panelPane != null) {
			wrap(panelPane);
			return wrapper;
		}

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

		ScrollPane nodeScrollPanel = new ScrollPane();
		nodeScrollPanel.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		nodeScrollPanel.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		nodeScrollPanel.setFitToHeight(true);
		nodeScrollPanel.setFitToWidth(true);
		nodeScrollPanel.setStyle("-fx-background-color:transparent;");
		nodeScrollPanel.setContent(gridPane);

		wrap(nodeScrollPanel);

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
			wrapper.setBottom(saveButton);
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

	void wrap(Region pane){
		if (wrapper == null) {
			wrapper = new BorderPane();
		} else {
			wrapper.getChildren().clear();
		}
		wrapper.setCenter(pane);

		pane.prefHeightProperty().bind(wrapper.prefHeightProperty());
		pane.minHeightProperty().bind(wrapper.minHeightProperty());
		pane.maxHeightProperty().bind(wrapper.maxHeightProperty());

		pane.prefWidthProperty().bind(wrapper.prefWidthProperty());
		pane.minWidthProperty().bind(wrapper.minWidthProperty());
		pane.maxWidthProperty().bind(wrapper.maxWidthProperty());

		panelPane = pane;
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
		if (namedControls == null) return;
		for (Map<String, Object> control : update) {
			String id = (String) control.get("id");
			if(id == null) continue;

			PluginOptionsControlItem item = namedControls.get(id);
			if (item == null) continue;

			Object value = control.get("value");
			if (value != null) namedControls.get(id).setValue(value);

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

	private static String getMsgTag(Map<String, Object> data){
		if (data.get("msgTag") != null) return (String) data.get("msgTag");
		return (String) data.get("onSave");
	}
}
