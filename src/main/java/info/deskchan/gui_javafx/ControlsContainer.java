package info.deskchan.gui_javafx;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlsContainer {
	
	final String name;
	List<Map<String, Object>> controls;
	String msgSave;
	String msgClose;
	private float columnGrow = 0.5f;
	private GridPane gridPane;

	ControlsContainer(String name, List<Map<String, Object>> controls, String msgSave, String msgClose) {
		this.name = name;
		update(controls, msgSave, msgClose);
		gridPane = new GridPane();
		gridPane.getStyleClass().add("grid-pane");
		float columnGrowPercentage = columnGrow * 100;
		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(columnGrowPercentage);
		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(95 - columnGrowPercentage);
		ColumnConstraints column3 = new ColumnConstraints();
		column3.setPercentWidth(5);
		gridPane.getColumnConstraints().addAll(column1, column2, column3);
	}

	void update(List<Map<String, Object>> controls, String msgSave, String msgClose) {
		this.controls = controls;
		this.msgSave = msgSave;
		this.msgClose = msgClose;
	}
	Node createControlsPane(Window parent) {
		gridPane.getChildren().clear();
		final Map<String, PluginOptionsControlItem> namedControls = new HashMap<>();
		BorderPane borderPane = new BorderPane();
		int row = 0;
		for (Map<String, Object> controlInfo : controls) {
			String id = (String) controlInfo.getOrDefault("id", null);
			String label = (String) controlInfo.getOrDefault("label", null);
			String hint = (String) controlInfo.getOrDefault("hint", null);
			PluginOptionsControlItem item = PluginOptionsControlItem.create(parent, controlInfo);
			if (item == null) {
				continue;
			}
			if (id != null) {
				namedControls.put(id, item);
				item.getNode().setId(id);
			}
			if (label == null) {
				gridPane.add(item.getNode(), 0, row, 2, 1);
			} else {
				Label labelNode = new Label(label + ":");
				labelNode.setWrapText(true);
				gridPane.add(labelNode, 0, row);
				gridPane.add(item.getNode(), 1, row);
			}
			if(hint!=null){
				gridPane.add(new Hint(hint),2,row);
			}
			row++;
		}
		if (msgSave != null) {
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
				Main.getInstance().getPluginProxy().sendMessage(msgSave, data);
			});
			borderPane.setBottom(saveButton);
		}
		if (msgClose != null) {
			parent.setOnCloseRequest(event -> {
				Main.getInstance().getPluginProxy().sendMessage(msgClose,null);
			});
		}
		borderPane.setTop(gridPane);
		return borderPane;
	}

	class Hint extends Label{
		Hint(String text){
			setText(" ❔ ");
			setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
			setTooltip(new Tooltip(text));
		}
	}
}
