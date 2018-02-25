package info.deskchan.gui_javafx;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.dialog.FontSelectorDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

interface PluginOptionsControlItem {
	
	default void init(Map<String, Object> options, Object value) { }
	
	default void setValue(Object value) { }
	
	default Object getValue() {  return null;  }

	default ObservableValue getProperty() {  return null;  }
	
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
			case "DirectoryField":
				item = new DirectoryFieldItem(parent);
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
			case "Separator":
				item = new SeparatorItem();
				break;
			case "Hyperlink":
				item = new HyperlinkItem();
				break;
		}

		if (item == null) {
			Main.log("Unknown type of item: "+type);
			return null;
		}
		item.init(options, value);

		Node node = item.getNode();
		if(width != null && height != null){
			width *= App.getInterfaceScale();
			height *= App.getInterfaceScale();
			node.setClip(new Rectangle(width, height));
		}
		if(width != null && node instanceof Region) {
			System.out.println("hi!");
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
		public void init(Map<String, Object> options, Object value) {
			if (options.containsKey("font")) {
				getStyleClass().remove("label");
				setFont(LocalFont.fromString(options.get("font").toString()));
			}
			try {
				setAlignment(Pos.valueOf(options.getOrDefault("align", "CENTER").toString().toUpperCase()));
			} catch (Exception e){
				setAlignment(Pos.CENTER_LEFT);
			}
			setValue(value);
		}

		@Override
		public void setValue(Object value) {
			if (value != null) setText(value.toString());
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

	class SeparatorItem extends Separator implements PluginOptionsControlItem {

		public SeparatorItem() {
			super();
		}

		@Override
		public Node getNode() {
			return this;
		}

	}

	class TextFieldItem implements PluginOptionsControlItem {

		TextField textField;
		String enterTag = null;
		Property currentText = new SimpleStringProperty();

		@Override
		public void init(Map<String, Object> options, Object value) {
			Boolean isPasswordField = (Boolean) options.getOrDefault("hideText", false);
			textField = (isPasswordField) ? new PasswordField() : new TextField();

			enterTag = (String)options.get("enterTag");
			setValue(value);

			textField.setOnKeyReleased(event -> {
				if (event.getCode() == KeyCode.ENTER) event();
			});
			textField.focusedProperty().addListener(
				(ObservableValue<? extends Boolean> arg0, Boolean oldValue, Boolean newValue) -> {
					if (!newValue) event();
			});
		}

		protected void event(){
			currentText.setValue(getValue());
			if (enterTag != null) {
				App.showWaitingAlert(() ->
						Main.getPluginProxy().sendMessage(enterTag, getValue())
				);
			}
		}

		@Override
		public void setValue(Object value) {
			textField.setText(value != null ? value.toString() : "");
		}

		@Override
		public Object getValue() {
			return textField.getText();
		}

		@Override
		public Node getNode() {
			return textField;
		}

		@Override
		public ObservableValue getProperty(){
			return currentText;
		}

	}

	class TextAreaItem implements PluginOptionsControlItem {

		TextArea area;
		@Override
		public void init(Map<String, Object> options, Object value) {
			area = new TextArea();
			area.setPadding(new Insets(2,2,2,2));
			Integer rowCount = (Integer) options.getOrDefault("rowCount", 5);
			area.setWrapText(true);
			area.setPrefRowCount(rowCount);
			setValue(value);
		}

		@Override
		public void setValue(Object value) {
			area.setText(value != null ? value.toString() : "");
		}

		@Override
		public Object getValue() { return area.getText(); }

		@Override
		public Node getNode() { return area; }
	}

	class CustomizableTextAreaItem implements PluginOptionsControlItem {

		ScrollPane scrollPane;
		TextFlow area;

		@Override
		public void init(Map<String, Object> options, Object value) {
			scrollPane = new ScrollPane();
			area = new TextFlow(new Text("Пустой текст"));
			area.setPadding(new Insets(0, 5, 0, 5));
			scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			scrollPane.setContent(area);
			scrollPane.setFitToWidth(true);

			area.prefHeightProperty().bind(scrollPane.heightProperty());
			area.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
			setValue(value);
		}

		@Override
		public void setValue(Object value) {
			area.getChildren().clear();
			if (value == null) return;

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
		public void init(Map<String, Object> options, Object value) {
			int min =  ((Number) options.getOrDefault("min",   0)).intValue();
			int max =  ((Number) options.getOrDefault("max", 100)).intValue();
			int step = ((Number) options.getOrDefault("step",  1)).intValue();
			spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, min, step));
			setValue(value);

			if (options.get("msgTag") != null){
				String msgTag = options.get("msgTag").toString();
				spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
					App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, newValue));
				});
			}
		}

		@Override
		public void setValue(Object value) {
			try {
				spinner.getValueFactory().setValue(((Number) value).intValue());
			} catch (Exception e){ }
		}

		@Override
		public Object getValue() {
			return spinner.getValue();
		}

		@Override
		public Node getNode() {
			return spinner;
		}

		@Override
		public ObservableValue getProperty(){
			return spinner.valueProperty();
		}
	}

	class FloatSpinnerItem implements PluginOptionsControlItem {

		private final Spinner<Double> spinner = new ImprovedSpinner<>();

		@Override
		public void init(Map<String, Object> options, Object value) {
			double min =  ((Number) options.getOrDefault("min",   0)).doubleValue();
			double max =  ((Number) options.getOrDefault("max", 100)).doubleValue();
			double step = ((Number) options.getOrDefault("step",  1)).doubleValue();
			setValue(value);

			spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, min, step));
			if (options.get("msgTag") != null){
				String msgTag = options.get("msgTag").toString();
				spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
					App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, newValue));
				});
			}
		}

		@Override
		public void setValue(Object value) {
			try {
				spinner.getValueFactory().setValue(((Number) value).doubleValue());
			} catch (Exception e) { }
		}

		@Override
		public Object getValue() {
			return spinner.getValue();
		}

		@Override
		public Node getNode() {
			return spinner;
		}

		@Override
		public ObservableValue getProperty(){
			return spinner.valueProperty();
		}

	}

	class SliderItem implements PluginOptionsControlItem {

		private Slider slider = new Slider();

		@Override
		public void init(Map<String, Object> options, Object value) {
			double min =  ((Number) options.getOrDefault("min",   0)).doubleValue();
			double max =  ((Number) options.getOrDefault("max", 100)).doubleValue();
			Double step = ((Number) options.getOrDefault("step",  (max-min) / 20.0)).doubleValue();

			slider.setMin(min);
			slider.setValue(min);
			slider.setMax(max);
			slider.setBlockIncrement(step);
			slider.setMinorTickCount(10);
			slider.setMajorTickUnit(step * 10);
			slider.setShowTickLabels(true);
			if (step > 1) {
				slider.setShowTickMarks(true);
				slider.setSnapToTicks(true);
			}
			setValue(value);

			if (options.get("msgTag") != null){
				String msgTag = options.get("msgTag").toString();
				slider.valueProperty().addListener((obs, oldValue, newValue) -> {
					App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, newValue));
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
			try {
				double val = ((Number) value).doubleValue();
				slider.setValue(val);
			} catch (Exception e){ }
		}

		@Override
		public Object getValue() {
			return slider.getValue();
		}

		@Override
		public Node getNode() {
			return slider;
		}

		@Override
		public ObservableValue getProperty(){
			return slider.valueProperty();
		}

	}

	class CheckBoxItem extends CheckBox implements PluginOptionsControlItem {

		@Override
		public void init(Map<String, Object> options, Object value) {
			setValue(value);

			String msgTag = (String) options.get("msgTag");
			if(msgTag != null){
				selectedProperty().addListener((obs, oldValue, newValue) -> {
					if(oldValue.equals(newValue)) return;
					App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, newValue));
				});
			}
		}
		@Override
		public void setValue(Object value) {
			setSelected(value != null ? (boolean) value : false);
		}

		@Override
		public Object getValue() {
			return isSelected();
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public ObservableValue getProperty(){
			return selectedProperty();
		}
	}

	class ComboBoxItem implements PluginOptionsControlItem {

		private final ComboBox<Object> comboBox = new ComboBox<>();

		@Override
		public void init(Map<String, Object> options, Object value) {
			List<Object> items = (List) options.get("values");
			comboBox.setItems(FXCollections.observableList(items));
			setValue(value);

			String msgTag=(String)options.get("msgTag");
			if(msgTag!=null){
				comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
					App.showWaitingAlert(() ->
						Main.getPluginProxy().sendMessage(msgTag, new HashMap<String,Object>(){{
							put("value", newValue.toString());
						}})
					);
				});
			}
		}

		@Override
		public void setValue(Object value) {
			comboBox.getSelectionModel().select(value != null ? (int) value : 0);
		}

		@Override
		public Object getValue() {
			return comboBox.getSelectionModel().getSelectedIndex();
		}

		@Override
		public Node getNode() {
			return comboBox;
		}

		@Override
		public ObservableValue getProperty(){
			return comboBox.valueProperty();
		}
	}

	class ListBoxItem implements PluginOptionsControlItem {

		private final ListView<Object> listView = new ListView<>();

		@Override
		public void init(Map<String, Object> options, Object value) {
			List<Object> items = (List) options.get("values");

			if(items != null && items.size( )> 0)
				listView.setItems(FXCollections.observableList(items));
			listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			listView.setPrefHeight(150);

			setValue(value);
			String msgTag = (String) options.get("msgTag");
			if(msgTag != null){
				listView.setOnMouseClicked((event) -> {
					App.showWaitingAlert(() ->
						Main.getPluginProxy().sendMessage(msgTag,new HashMap<String,Object>(){{
							put("value", new ArrayList<>(listView.getSelectionModel().getSelectedItems()));
							put("index", new ArrayList<>(listView.getSelectionModel().getSelectedIndex()));
						}})
					);
				});
			}
		}

		@Override
		public void setValue(Object value) {
			if (value == null || !(value instanceof List)) return;
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

		@Override
		public ObservableValue getProperty(){
			return listView.getSelectionModel().selectedIndexProperty();
		}

	}

	class ButtonItem extends Button implements PluginOptionsControlItem {

		@Override
		public void init(Map<String, Object> options, Object value) {
			String msgTag = (String) options.get("msgTag");
			Object msgData = options.get("msgData");
			Object dstPanel = options.get("dstPanel");

			setValue(value);
			if (msgTag != null) {
				setOnAction(event ->
						App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, msgData))
				);
			} else if (dstPanel != null){
				setOnAction(event ->
						App.showWaitingAlert(() -> ControlsPanel.open(dstPanel.toString()))
				);
			}
		}

		@Override
		public void setValue(Object value) {
			setText(value != null ? value.toString() : "");
		}

		@Override
		public Object getValue() {
			return getText();
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public ObservableValue getProperty(){
			return textProperty();
		}
	}

	class FilesManagerItem extends Button implements PluginOptionsControlItem {
		private List<String> files;
		private Window parent;

		public FilesManagerItem(Window window){
			parent = window;
		}

		@Override
		public void init(Map<String, Object> options, Object value) {
			setText(Main.getString("choose"));
			files = new ArrayList<>();
			try {
				files = (List<String>) options.get("filesList");
			} catch (Exception e) {
				e.printStackTrace();
			}
			setValue(value);
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
			if (value != null && value instanceof List) files = (List<String>) value;
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

	abstract class FileSystemChooserItem extends BorderPane implements PluginOptionsControlItem {
		protected final TextField textField = new TextField();
		protected final Button selectButton = new Button("...");
		protected final Button clearButton = new Button("X");
		protected Window parent;

		FileSystemChooserItem(Window parent) {
			textField.setEditable(false);
			setLeft(textField);
			clearButton.setOnAction(event -> textField.clear());
			setRight(clearButton);
			setCenter(selectButton);
			setMaxHeight(textField.getHeight());
			this.parent = parent;
		}

		@Override
		public void setValue(Object value) {
			textField.setText(value != null ? value.toString() : "");
		}

		@Override
		public Object getValue() {
			return textField.getText();
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public ObservableValue getProperty(){
			return textField.textProperty();
		}
	}

	class FileFieldItem extends FileSystemChooserItem {
		private final FileChooser chooser = new FileChooser();

		FileFieldItem(Window parent) {	super(parent); 	}

		@Override
		public void init(Map<String, Object> options, Object value) {
			String path = (String) options.get("initialDirectory");
			if (path != null) {
				chooser.setInitialDirectory(new File(path));
			}
			setValue(value);

			final String msgTag = (String) options.get("msgTag");
			selectButton.setOnAction(event -> {
				File file = chooser.showOpenDialog(parent);
				if (file != null) {
					textField.setText(file.getAbsolutePath());
					if (msgTag != null)
						App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, textField.getText()));
				}
			});

			List<Map<String, Object>> filters = (List) options.getOrDefault("filters", new ArrayList<>(0));
			for (Map<String, Object> filter : filters) {
				String description = (String) filter.get("description");
				List<String> extensions = (List) filter.get("extensions");
				if (description != null && extensions != null) {
					chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
				}
			}
		}
	}

	class DirectoryFieldItem extends FileSystemChooserItem {
		private final DirectoryChooser chooser = new DirectoryChooser();

		DirectoryFieldItem(Window parent) {	 super(parent);  }

		@Override
		public void init(Map<String, Object> options, Object value) {
			String path = (String) options.get("initialDirectory");
			if (path != null) {
				chooser.setInitialDirectory(new File(path));
			}
			setValue(value);

			final String msgTag = (String) options.get("msgTag");
			selectButton.setOnAction(event -> {
				File file = chooser.showDialog(parent);
				if (file != null) {
					textField.setText(file.getAbsolutePath());
					if (msgTag != null)
						App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, textField.getText()));
				}
			});
		}
	}

	class DatePickerItem implements PluginOptionsControlItem {

		DatePicker picker = new DatePicker();
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

		@Override
		public void init(Map<String, Object> options, Object value) {
			String format = (String) options.get("format");
			if (format != null) {
				formatter = DateTimeFormatter.ofPattern(format);
			}
			setValue(value);
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

		@Override
		public ObservableValue getProperty(){
			return picker.valueProperty();
		}
	}

	class ColorPickerItem implements PluginOptionsControlItem {

		ColorPicker picker = new ColorPicker();

		@Override
		public void init(Map<String, Object> options, Object value) {
			String msgTag = (String) options.get("msgTag");
			setValue(value);
			if (msgTag != null) {
				picker.setOnAction(event -> {
					App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, picker.getValue().toString()));
				});
			}
		}

		@Override
		public Object getValue() {
			return picker.getValue().toString();
		}

		@Override
		public void setValue(Object value) {
			if (value == null){
				picker.setValue(Color.BLACK);
				return;
			}
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

		@Override
		public ObservableValue getProperty(){
			return picker.valueProperty();
		}
	}

	class FontPickerItem extends ButtonItem {

		FontSelectorDialog picker;
		String msgTag;
		private Window parent;
		Property<String> selectedFont = new SimpleStringProperty();

		public FontPickerItem(Window window){
			parent = window;
		}

		@Override
		public void init(Map<String, Object> options, Object value) {
			setValue(value);
			msgTag = (String) options.get("msgTag");

			setOnAction(event -> {
				Stage stage = (Stage) picker.getDialogPane().getScene().getWindow();
				stage.setAlwaysOnTop(TemplateBox.checkForceOnTop());
				Optional<Font> selectedFontOpt = picker.showAndWait();
				if (selectedFontOpt.isPresent()) {
					selectedFont.setValue(LocalFont.toString(selectedFontOpt.get()));
					setText(selectedFont.getValue());
					if (msgTag != null)
						App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, getValue()));
					picker = new FontSelectorDialog(selectedFontOpt.get());
				} else {
					picker = new FontSelectorDialog(picker.getResult());
				}
				picker.setResizable(true);
				picker.getDialogPane().setStyle(LocalFont.getDefaultFontCSS());

			});
		}

		@Override
		public Object getValue() {
			return selectedFont.getValue();
		}

		@Override
		public void setValue(Object value) {
			selectedFont.setValue((String) value);
			try {
				if (selectedFont != null) {
					Font font = LocalFont.fromString(selectedFont.getValue());
					picker = new FontSelectorDialog(font);
					setText(selectedFont.getValue());
					picker.initOwner(parent);
					return;
				}
			} catch (Exception e){ }

			picker = new FontSelectorDialog(LocalFont.defaultFont);
			setText(Main.getString("default"));
			picker.initOwner(parent);
			picker.setResizable(true);
			picker.getDialogPane().setStyle(LocalFont.getDefaultFontCSS());
		}

		@Override
		public ObservableValue getProperty(){
			return selectedFont;
		}
	}

	class HyperlinkItem extends Hyperlink implements PluginOptionsControlItem {

		String link;

		@Override
		public void init(Map<String, Object> options, Object value) {
			String msgTag = (String) options.get("msgTag");
			link = (String) options.get("value");
			if (link != null) link = wrap(link);
			setValue(Main.getString("open"));

			setOnAction(event -> {
				if (msgTag != null)
					App.showWaitingAlert(() -> Main.getPluginProxy().sendMessage(msgTag, null));
				if (link != null){
					String[] command;
					if (SystemUtils.IS_OS_WINDOWS)
						command = new String[]{ "cmd", "/c", "start " + link };
					else if (SystemUtils.IS_OS_LINUX)
						command = new String[]{ "xdg-open", link };
					else if (SystemUtils.IS_OS_MAC)
						command = new String[]{ "open", link };
					else return;
					ProcessBuilder builder = new ProcessBuilder( command );
					builder.redirectErrorStream(true);
					try {
						Process p = builder.start();
						BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
						String line;
						while (true) {
							line = r.readLine();
							if (line == null) {
								break;
							}
							System.out.println(line);
						}
					} catch (Exception e){
						Main.log(e);
					}
				}
			});
		}

		@Override
		public Object getValue() {
			return link;
		}

		@Override
		public void setValue(Object value) { link = (String) value; }

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public ObservableValue getProperty(){ return null; }

		String wrap(String val){
			if(val.charAt(0) != '"')
				val = '"' + val;
			if(val.charAt(val.length() - 1) != '"')
				val = val+'"';
			return val;
		}
	}
}
