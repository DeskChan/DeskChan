package info.deskchan.gui_javafx;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlsContainer {
	
	final String name;
	List<Map<String, Object>> controls;
	String msgTag;
	float columnGrow;
	Window parent;
	ControlsContainer(String name, List<Map<String, Object>> controls, String msgTag) {
		this.name = name;
		this.controls = controls;
		this.msgTag = msgTag;
		this.parent=null;
		columnGrow=0.4f;
	}
	void setParent(Window parent){
		this.parent=parent;
	}
	void update(List<Map<String, Object>> controls, String msgTag, float columnGrow) {
		this.controls = controls;
		this.msgTag = msgTag;
		this.columnGrow = columnGrow;
	}
	
	Node createControlsPane() {
		if(parent==null)
			parent=OptionsDialog.getInstance().getDialogPane().getScene().getWindow();
		final Map<String, PluginOptionsControlItem> namedControls = new HashMap<>();
		BorderPane borderPane = new BorderPane();
		GridPane gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");
		float columnGrowPercentage = columnGrow * 100;
		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(columnGrowPercentage);
		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(100 - columnGrowPercentage);
		gridPane.getColumnConstraints().addAll(column1, column2);

		int row = 0;
		for (Map<String, Object> controlInfo : controls) {
			String id = (String) controlInfo.getOrDefault("id", null);
			String label = (String) controlInfo.getOrDefault("label", null);
			PluginOptionsControlItem item = PluginOptionsControlItem.create(parent,controlInfo);
			if (item == null) {
				continue;
			}
			if (id != null) {
				namedControls.put(id, item);
			}
			if (label == null) {
				gridPane.add(item.getNode(), 0, row, 2, 1);
			} else {
				gridPane.add(new Label(label + ":"), 0, row);
				gridPane.add(item.getNode(), 1, row);
			}
			row++;
		}
		if (msgTag != null) {
			Button saveButton = new Button(Main.getString("save"));
			saveButton.setOnAction(event -> {
				Map<String, Object> data = new HashMap<>();
				for (Map.Entry<String, PluginOptionsControlItem> entry : namedControls.entrySet()) {
					data.put(entry.getKey(), entry.getValue().getValue());
					for (Map<String, Object> control : controls) {
						String id = (String) control.getOrDefault("id", null);
						if (id != null) {
							if (id.equals(entry.getKey())) {
								control.put("value", entry.getValue().getValue());
								break;
							}
						}
					}
				}
				Main.getInstance().getPluginProxy().sendMessage(msgTag, data);
			});
			gridPane.add(saveButton, 0, row, 2, 1);
		}
		borderPane.setTop(gridPane);
		return borderPane;
	}
	
}
