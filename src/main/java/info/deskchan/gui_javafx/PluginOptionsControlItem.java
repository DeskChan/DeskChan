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
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.dialog.FontSelectorDialog;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

interface PluginOptionsControlItem {
	
	default void init(Map<String, Object> options) { }
	
	default void setValue(Object value) { }
	
	default Object getValue() {  return null;  }
	
	Node getNode();
	
	static PluginOptionsControlItem create(Window parent, Map<String, Object> options) {
		String type   = (String) options.get("type");
		Object value  = options.get("value");
		Double width  = App.getDouble(options.get("width"), null);
		Double height = App.getDouble(options.get("height"), null);
		Boolean disabled = App.getBoolean(options.get("disabled"), null);

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
			case "Slider":
				item = new SliderItem();
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
			case "ColorPicker":
				item = new ColorPickerItem();
				break;
			case "FontPicker":
				item = new FontPickerItem(parent);
				break;
		}

		if (item == null) {
			Main.log("Unknown type of item: "+type);
			return null;
		}
		item.init(options);
		if (value != null) {
			item.setValue(value);
		}

		Node node = item.getNode();
		if(width != null && height != null){
			width *= App.getInterfaceMultiplierSize();
			height *= App.getInterfaceMultiplierSize();
			node.setClip(new Rectangle(width, height));
		}
		if(width != null && node instanceof Region) {
			((Region) node).setMinWidth(width);
			((Region) node).setMaxWidth(width);
		}
		if(height != null && node instanceof Region) {
			((Region) node).setMinHeight(height);
			((Region) node).setMaxHeight(height);
		}

		if(disabled != null){
			node.setDisable(disabled);
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
			setFont(LocalFont.defaultFont);
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

			enterTag = (String)options.get("enterTag");
			if(enterTag != null){
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
			area = new TextArea();
			area.setPadding(new Insets(5,5,5,5));
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

		ScrollPane scrollPane;
		TextFlow area;

		@Override
		public void init(Map<String, Object> options) {
			scrollPane = new ScrollPane();
			area = new TextFlow(new Text("Пустой текст"));
			area.setPadding(new Insets(0, 5, 0, 5));
			scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			scrollPane.setContent(area);
			scrollPane.setFitToWidth(true);

			area.prefHeightProperty().bind(scrollPane.heightProperty());
			area.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		}

		@Override
		public void setValue(Object value) {
			area.getChildren().clear();
			if(value instanceof String){
				Text t = new Text((String) value);
				area.getChildren().add(t);
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
						if (map.containsKey("font")) {
							Font font = LocalFont.fromString((String) map.get("font"));
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
			area.getChildren().addAll(list);
		}

		@Override
		public Object getValue() { return area.getChildren(); }

		@Override
		public Node getNode() { return scrollPane; }
	}

	class ImprovedSpinner<T> extends Spinner<T> {
		ImprovedSpinner() {
			super();
			setOnScroll(event -> {
				if (event.getDeltaY() > 0) {
					increment();
				} else if (event.getDeltaY() < 0) {
					decrement();
				}
			});
			setEditable(true);
			focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (!newValue) {
					increment(0); // won't change value, but will commit editor
				}
			});
		}
	}

	class IntSpinnerItem implements PluginOptionsControlItem {
		
		private final Spinner<Integer> spinner = new ImprovedSpinner<>();

		@Override
		public void init(Map<String, Object> options) {
			int min =  ((Number) options.getOrDefault("min",   0)).intValue();
			int max =  ((Number) options.getOrDefault("max", 100)).intValue();
			int step = ((Number) options.getOrDefault("step",  1)).intValue();
			spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, min, step));

			if (options.get("msgTag") != null){
				String msgTag = options.get("msgTag").toString();
				spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag, newValue);
				});
			}
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

	class FloatSpinnerItem implements PluginOptionsControlItem {

		private final Spinner<Double> spinner = new ImprovedSpinner<>();

		@Override
		public void init(Map<String, Object> options) {
			double min =  ((Number) options.getOrDefault("min",   0)).doubleValue();
			double max =  ((Number) options.getOrDefault("max", 100)).doubleValue();
			double step = ((Number) options.getOrDefault("step",  1)).doubleValue();

			spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, min, step));
			if (options.get("msgTag") != null){
				String msgTag = options.get("msgTag").toString();
				spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag, newValue);
				});
			}
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

	class SliderItem implements PluginOptionsControlItem {

		private Slider slider = new Slider();

		@Override
		public void init(Map<String, Object> options) {
			double min =  ((Number) options.getOrDefault("min",   0)).doubleValue();
			double max =  ((Number) options.getOrDefault("max", 100)).doubleValue();
			Double step = ((Number) options.getOrDefault("step",  (max-min) / 20.0)).doubleValue();

			slider.setMin(min);
			slider.setMax(max);
			slider.setBlockIncrement(step);
			slider.setMinorTickCount(10);
			slider.setMajorTickUnit(step * 10);
			slider.setShowTickLabels(true);
			if (step > 1) {
				slider.setShowTickMarks(true);
				slider.setSnapToTicks(true);
			}

			if (options.get("msgTag") != null){
				String msgTag = options.get("msgTag").toString();
				slider.valueProperty().addListener((obs, oldValue, newValue) -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag, newValue);
				});
			}

			slider.setOnScroll(event -> {
				if (event.getDeltaY() > 0) {
					slider.increment();
				} else if (event.getDeltaY() < 0) {
					slider.decrement();
				}
			});
		}

		@Override
		public void setValue(Object value) {
			double val = ((Number) value).doubleValue();
			slider.setValue(val);
		}

		@Override
		public Object getValue() {
			return slider.getValue();
		}

		@Override
		public Node getNode() {
			return slider;
		}

	}

	class CheckBoxItem extends CheckBox implements PluginOptionsControlItem {

		@Override
		public void init(Map<String, Object> options) {
			String msgTag = (String) options.get("msgTag");
			if(msgTag != null){
				selectedProperty().addListener((obs, oldValue, newValue) -> {
					if(oldValue.equals(newValue)) return;
					Main.getInstance().getPluginProxy().sendMessage(msgTag, newValue);
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
			List<Object> items = (List) options.get("values");
			comboBox.setItems(FXCollections.observableList(items));
			String msgTag=(String)options.get("msgTag");
			if(msgTag!=null){
				comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag, new HashMap<String,Object>(){{
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
			List<Object> items = (List) options.get("values");
			if(items!=null && items.size()>0)
				listView.setItems(FXCollections.observableList(items));
			listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			listView.setPrefHeight(150);
			String msgTag=(String)options.get("msgTag");
			if(msgTag!=null){
				listView.setOnMouseClicked((event) -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag,new HashMap<String,Object>(){{
						put("value", new ArrayList<>(listView.getSelectionModel().getSelectedItems()));
						put("index", new ArrayList<>(listView.getSelectionModel().getSelectedIndex()));
					}});
				});

			}
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
			String msgTag = (String) options.get("msgTag");
			Object msgData = options.get("msgData");
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
			setLeft(textField);
			clearButton.setOnAction(event -> textField.clear());
			setRight(clearButton);
			selectButton.setOnAction(event -> {
				File file = chooser.showOpenDialog(parent);
				if (file != null) {
					textField.setText(file.getAbsolutePath());
				}
			});
			setCenter(selectButton);
			setMaxHeight(textField.getHeight());
		}
		
		@Override
		public void init(Map<String, Object> options) {
			String path = (String) options.get("initialDirectory");
			if (path != null) {
				chooser.setInitialDirectory(new File(path));
			}

			List<Map<String, Object>> filters = (List) options.getOrDefault("filters", new ArrayList<>(0));
			for (Map<String, Object> filter : filters) {
				String description = (String) filter.get("description");
				List<String> extensions = (List) filter.get("extensions");
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
			String format = (String) options.get("format");
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

	class ColorPickerItem implements PluginOptionsControlItem {

		ColorPicker picker = new ColorPicker();

		@Override
		public void init(Map<String, Object> options) {
			String msgTag = (String) options.get("msgTag");
			if (msgTag != null) {
				picker.setOnAction(event -> {
					Main.getInstance().getPluginProxy().sendMessage(msgTag, picker.getValue().toString());
				});
			}
		}

		@Override
		public Object getValue() {
			return picker.getValue().toString();
		}

		@Override
		public void setValue(Object value) {
			String colorText = (String) value;
			colorText = colorText.toUpperCase();
			try {
				picker.setValue(Color.valueOf(colorText));
			} catch (Exception e){
				try {
					picker.setValue(Color.web(colorText));
				} catch (Exception e2){
					picker.setValue(Color.BLACK);
				}
			}
		}

		@Override
		public Node getNode() {
			return picker;
		}
	}

	class FontPickerItem extends ButtonItem {

		FontSelectorDialog picker;
		String msgTag;
		String selectedFont;
		private Window parent;

		public FontPickerItem(Window window){
			parent = window;
		}

		@Override
		public void init(Map<String, Object> options) {
			setValue(options.get("font"));
			msgTag = (String) options.get("msgTag");

			setOnAction(event -> {
				Stage stage = (Stage) picker.getDialogPane().getScene().getWindow();
				stage.setAlwaysOnTop(true);
				Optional<Font> selectedFontOpt = picker.showAndWait();
				if (selectedFontOpt.isPresent()) {
					selectedFont = LocalFont.toString(selectedFontOpt.get());
					setText(selectedFont);
					if (msgTag != null)
						Main.getInstance().getPluginProxy().sendMessage(msgTag, getValue());
					picker = new FontSelectorDialog(selectedFontOpt.get());
				} else {
					picker = new FontSelectorDialog(picker.getResult());
				}
			});
		}

		@Override
		public Object getValue() {
			return selectedFont;
		}

		@Override
		public void setValue(Object value) {
			selectedFont = (String) value;
			try {
				if (selectedFont != null) {
					Font font = LocalFont.fromString(selectedFont);
					picker = new FontSelectorDialog(font);
					setText(selectedFont);
					picker.initOwner(parent);
					return;
				}
			} catch (Exception e){ }

			picker = new FontSelectorDialog(LocalFont.defaultFont);
			setText(Main.getString("default"));
			picker.initOwner(parent);
		}
	}
}
