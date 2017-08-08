package info.deskchan.gui_javafx;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

interface PluginOptionsControlItem {
	
	default void init(Map<String, Object> options) {
	}
	
	default void setValue(Object value) {
	}
	
	default Object getValue() {
		return null;
	}
	
	Node getNode();
	
	static PluginOptionsControlItem create(Window parent, Map<String, Object> options) {
		String type = (String) options.get("type");
		Object value = options.getOrDefault("value", null);
		Double width=(Double)options.getOrDefault("width",null);
		Double height=(Double)options.getOrDefault("height",null);
		PluginOptionsControlItem item = null;
		switch (type) {
			case "Label":
				item = new LabelItem();
				break;
			case "TextField":
				item = new TextFieldItem();
				break;
			case "Spinner":
			case "IntSpinner":
				item = new IntSpinnerItem();
				break;
			case "FloatSpinner":
				item = new FloatSpinnerItem();
				break;
			case "CheckBox":
				item = new CheckBoxItem();
				break;
			case "ComboBox":
				item = new ComboBoxItem();
				break;
			case "ListBox":
				item = new ListBoxItem();
				break;
			case "Button":
				item = new ButtonItem();
				break;
			case "FileField":
				item = new FileFieldItem(parent);
				break;
			case "DatePicker":
				item = new DatePickerItem();
				break;
			case "FilesManager":
				item = new FilesManagerItem(parent);
				break;
			case "TextArea":
				item = new TextAreaItem();
				break;
			case "CustomizableTextArea":
				item = new CustomizableTextAreaItem();
				break;
		}
		if (item == null) {
			return null;
		}
		item.init(options);
		if (value != null) {
			item.setValue(value);
		}
		Node node=item.getNode();
		if(width!=null && height!=null){
			node.setClip(new Rectangle(width,height));
		}
		if(width!=null && node instanceof Region) {
			((Region) node).setMinWidth(width);
			((Region) node).setMaxWidth(width);
		}
		if(height!=null && node instanceof Region) {
			((Region) node).setMinHeight(height);
			((Region) node).setMaxHeight(height);
		}
		return item;
	}
	
	class LabelItem extends Label implements PluginOptionsControlItem {

		public LabelItem() {
			super();
			setWrapText(true);
		}

		@Override
		public void setValue(Object value) {
			setText(value.toString());
		}
		
		@Override
		public Object getValue() {
			return getText();
		}
		
		@Override
		public Node getNode() {
			return this;
		}
		
	}
	
	class TextFieldItem implements PluginOptionsControlItem {
		
		TextField textField;
		String enterTag=null;
		@Override
		public void init(Map<String, Object> options) {
			Boolean isPasswordField = (Boolean) options.getOrDefault("hideText", false);
			textField = (isPasswordField) ? new PasswordField() : new TextField();
			enterTag=(String)options.getOrDefault("enterTag",null);
			if(enterTag!=null){
				textField.setOnKeyReleased(event -> {
					if (event.getCode() == KeyCode.ENTER){
						Main.getInstance().getPluginProxy().sendMessage(enterTag,new HashMap<String,Object>(){{
							put("value", getValue());
						}});
					}
				});
			}
		}

		@Override
		public void setValue(Object value) {
			textField.setText(value.toString());
		}
		
		@Override
		public Object getValue() {
			return textField.getText();
		}
		
		@Override
		public Node getNode() {
			return textField;
		}
		
	}
	class TextAreaItem implements PluginOptionsControlItem {

		TextArea area;
		@Override
		public void init(Map<String, Object> options) {
			area=new TextArea();
			Integer rowCount = (Integer) options.getOrDefault("rowCount", 5);
			area.setPrefRowCount(rowCount);
		}

		@Override
		public void setValue(Object value) { area.setText(value.toString()); }

		@Override
		public Object getValue() { return area.getText(); }

		@Override
		public Node getNode() { return area; }
	}
	class CustomizableTextAreaItem implements PluginOptionsControlItem {

		TextFlow area;

		@Override
		public void init(Map<String, Object> options) {
			area=new TextFlow(new Text("Пустой текст"));
			idk();
		}

		@Override
		public void setValue(Object value) {
			if(value instanceof String){
				Text t=new Text((String)value);
				area=new TextFlow(t);
				return;
			}
			List<Object> values;
			if(value instanceof List) {
				values = (List<Object>) value;
			} else {
				values = new ArrayList<>();
				if (value instanceof Map)
					values.add(value);
			}
			ArrayList<Text> list=new ArrayList<>();
			for(Object cur : values){
				if(cur instanceof String)
					list.add(new Text((String)cur));
				if(cur instanceof Map){
					Text t=new Text();
					try {
						Map<Object, Object> map = (Map<Object, Object>) cur;
						if (map.containsKey("text"))
							t.setText((String) map.get("text"));
						if (map.containsKey("color"))
							t.setFill(Paint.valueOf((String) map.get("color")));
						if (map.containsKey("size")) {
							Font font = t.getFont();
							font = Font.font(font.getFamily(), FontWeight.findByName(font.getStyle()), (Integer) map.get("size"));
							t.setFont(font);
						}
						if (map.containsKey("style")) {
							Font font = t.getFont();
							font = Font.font(font.getFamily(), FontWeight.findByName((String) map.get("style")), font.getSize());
							t.setFont(font);
						}
						list.add(t);
					} catch (Exception e){ }
				}
			}
			area.getChildren().clear();
			area.getChildren().addAll(list);
			idk();
		}

		private void idk(){
			area.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
			area.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		}
		@Override
		public Object getValue() { return area.getChildren(); }

		@Override
		public Node getNode() { return area; }
	}

	abstract class SpinnerItem implements PluginOptionsControlItem {

		protected void configureOnChangeAction(Spinner<? extends Number> spinner, Map<String, Object> configuration) {
			if(configuration != null && configuration.containsKey("msgTag")) {
				String msgTag = (String) configuration.get("msgTag");
				String newValueField = (String) configuration.getOrDefault("newValueField", "value");
				String oldValueField = (String) configuration.getOrDefault("oldValueField", "oldValue");
				double multiplier = ((Number) configuration.getOrDefault("multiplier", 1.0)).doubleValue();
				Map<String, Object> data;
				if (configuration.containsKey("data")) {
					data = (Map<String, Object>) configuration.get("data");
				} else {
					data = null;
				}

				spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
					Map<String, Object> eventData = new HashMap<>();
					eventData.put(newValueField, newValue.doubleValue() * multiplier);
					eventData.put(oldValueField, oldValue.doubleValue());
					if (data != null) {
						data.forEach(eventData::put);
					}
					Main.getInstance().getPluginProxy().sendMessage(msgTag, eventData);
				});
			}
		}

	}

	class IntSpinnerItem extends SpinnerItem {
		
		private final Spinner<Integer> spinner = new Spinner<>();

		@Override
		public void init(Map<String, Object> options) {
			int min = ((Number) options.getOrDefault("min", 0)).intValue();
			int max = ((Number) options.getOrDefault("max", 100)).intValue();
			int step = ((Number) options.getOrDefault("step", 1)).intValue();
			spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, min, step));
			Map<String, Object> onChangeMap = (Map<String, Object>) options.getOrDefault("onChange",null);
			configureOnChangeAction(spinner, onChangeMap);
		}
		
		@Override
		public void setValue(Object value) {
			spinner.getValueFactory().setValue(((Number) value).intValue());
		}
		
		@Override
		public Object getValue() {
			return spinner.getValue();
		}
		
		@Override
		public Node getNode() {
			return spinner;
		}
		
	}

	class FloatSpinnerItem extends SpinnerItem {

		private final Spinner<Double> spinner = new Spinner<>();

		@Override
		public void init(Map<String, Object> options) {
			double min = ((Number) options.getOrDefault("min", 0)).doubleValue();
			double max = ((Number) options.getOrDefault("max", 100)).doubleValue();
			double step = ((Number) options.getOrDefault("step", 1)).doubleValue();
			spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, min, step));
			Map<String, Object> onChangeMap = (Map<String, Object>) options.getOrDefault("onChange",null);
			configureOnChangeAction(spinner, onChangeMap);
		}

		@Override
		public void setValue(Object value) {
			spinner.getValueFactory().setValue(((Number) value).doubleValue());
		}

		@Override
		public Object getValue() {
			return spinner.getValue();
		}

		@Override
		public Node getNode() {
			return spinner;
		}

	}

	class CheckBoxItem extends CheckBox implements PluginOptionsControlItem {

		@Override
		public void init(Map<String, Object> options) {
			String msgTag=(String)options.getOrDefault("msgTag",null);
			if(msgTag!=null){
				selectedProperty().addListener((obs, oldValue, newValue) -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag,new HashMap<String,Object>(){{
						put("value", newValue);
					}});
				});
			}
		}
		@Override
		public void setValue(Object value) { setSelected((boolean) value); }

		@Override
		public Object getValue() {
			return isSelected();
		}

		@Override
		public Node getNode() {
			return this;
		}

	}
	
	class ComboBoxItem implements PluginOptionsControlItem {
		
		private final ComboBox<Object> comboBox = new ComboBox<>();
		
		@Override
		public void init(Map<String, Object> options) {
			List<Object> items = (List<Object>) options.get("values");
			comboBox.setItems(FXCollections.observableList(items));
			String msgTag=(String)options.getOrDefault("msgTag",null);
			if(msgTag!=null){
				comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag,new HashMap<String,Object>(){{
						put("value", newValue.toString());
					}});
				});
			}
		}
		
		@Override
		public void setValue(Object value) {
			comboBox.getSelectionModel().select((int) value);
		}
		
		@Override
		public Object getValue() {
			return comboBox.getSelectionModel().getSelectedIndex();
		}
		
		@Override
		public Node getNode() {
			return comboBox;
		}
		
	}
	
	class ListBoxItem implements PluginOptionsControlItem {
		
		private final ListView<Object> listView = new ListView<>();
		
		@Override
		public void init(Map<String, Object> options) {
			List<Object> items = (List<Object>) options.get("values");
			listView.setItems(FXCollections.observableList(items));
			listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			listView.setPrefHeight(150);
		}
		
		@Override
		public void setValue(Object value) {
			List<Integer> l = (List<Integer>) value;
			listView.getSelectionModel().clearSelection();
			for (Integer i : l) {
				if (i == null) {
					continue;
				}
				listView.getSelectionModel().select((int) i);
			}
		}
		
		@Override
		public Object getValue() {
			return Arrays.asList(listView.getSelectionModel().getSelectedIndices().toArray());
		}
		
		@Override
		public Node getNode() {
			return listView;
		}
		
	}
	
	class ButtonItem extends Button implements PluginOptionsControlItem {
		
		@Override
		public void init(Map<String, Object> options) {
			String msgTag = (String) options.getOrDefault("msgTag", null);
			Object msgData = options.getOrDefault("msgData", null);
			if (msgTag != null) {
				setOnAction(event -> Main.getInstance().getPluginProxy().sendMessage(msgTag, msgData));
			}
		}
		
		@Override
		public void setValue(Object value) {
			setText(value.toString());
		}
		
		@Override
		public Object getValue() {
			return getText();
		}
		
		@Override
		public Node getNode() {
			return this;
		}
		
	}

	class FilesManagerItem extends Button implements PluginOptionsControlItem {
		private List<String> files;
		private Window parent;

		public FilesManagerItem(Window window){
			parent = window;
		}

		@Override
		public void init(Map<String, Object> options) {
			setText(Main.getString("choose"));
			files = new ArrayList<>();
			try {
				files = (List<String>) options.get("filesList");
			} catch (Exception e) {
				e.printStackTrace();
			}
			setOnAction(event -> callAction());
		}

		private void callAction(){
			FilesManagerDialog dialog = new FilesManagerDialog(parent,files);
			dialog.requestFocus();
			dialog.showAndWait();
			files = dialog.getFilesList();
		}

		@Override
		public void setValue(Object value) {
			files = (List<String>) value;
		}

		@Override
		public Object getValue() {
			return files;
		}

		@Override
		public Node getNode() {
			return this;
		}

	}

	class FileFieldItem extends BorderPane implements PluginOptionsControlItem {
		
		private final TextField textField = new TextField();
		private final Button selectButton = new Button("...");
		private final Button clearButton = new Button("X");
		private final FileChooser chooser = new FileChooser();
		
		FileFieldItem(Window parent) {
			textField.setEditable(false);
			setCenter(textField);
			clearButton.setOnAction(event -> textField.clear());
			setLeft(clearButton);
			selectButton.setOnAction(event -> {
				File file = chooser.showOpenDialog(parent);
				if (file != null) {
					textField.setText(file.getAbsolutePath());
				}
			});
			setRight(selectButton);
			setMaxHeight(textField.getHeight());
		}
		
		@Override
		public void init(Map<String, Object> options) {
			String path = (String) options.getOrDefault("initialDirectory", null);
			if (path != null) {
				chooser.setInitialDirectory(new File(path));
			}

			List<Map<String, Object>> filters = (List<Map<String, Object>>) options.getOrDefault("filters", new ArrayList<>(0));
			for (Map<String, Object> filter : filters) {
				String description = (String) filter.getOrDefault("description", null);
				List<String> extensions = (List<String>) filter.getOrDefault("extensions", null);
				if (description != null && extensions != null) {
					chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
				}
			}
		}
		
		@Override
		public void setValue(Object value) {
			textField.setText(value.toString());
		}
		
		@Override
		public Object getValue() {
			return textField.getText();
		}
		
		@Override
		public Node getNode() {
			return this;
		}
		
	}
	
	class DatePickerItem implements PluginOptionsControlItem {
		
		DatePicker picker = new DatePicker();
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
		
		@Override
		public void init(Map<String, Object> options) {
			String format = (String) options.getOrDefault("format", null);
			if (format != null) {
				formatter = DateTimeFormatter.ofPattern(format);
			}
		}
		
		@Override
		public Object getValue() {
			return picker.getValue().format(formatter);
		}
		
		@Override
		public void setValue(Object value) {
			LocalDate date;
			try {
				date = LocalDate.parse(value.toString(), formatter);
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				return;
			}
			
			picker.setValue(date);
		}
		
		@Override
		public Node getNode() {
			return picker;
		}
		
	}
	
}
