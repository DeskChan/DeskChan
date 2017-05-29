package info.deskchan.gui_javafx;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
		PluginOptionsControlItem item = null;
		switch (type) {
			case "Label":
				item = new LabelItem();
				break;
			case "TextField":
				item = new TextFieldItem();
				break;
			case "Spinner":
				item = new SpinnerItem();
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
		}
		if (item == null) {
			return null;
		}
		item.init(options);
		if (value != null) {
			item.setValue(value);
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
		
		@Override
		public void init(Map<String, Object> options) {
			Boolean isPasswordField = (Boolean) options.getOrDefault("hideText", false);
			textField = (isPasswordField) ? new PasswordField() : new TextField();
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
	
	class SpinnerItem implements PluginOptionsControlItem {
		
		private final Spinner<Integer> spinner = new Spinner<>();
		
		@Override
		public void init(Map<String, Object> options) {
			Integer min = (Integer) options.getOrDefault("min", 0);
			Integer max = (Integer) options.getOrDefault("max", 100);
			Integer step = (Integer) options.getOrDefault("step", 1);
			spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, min, step));
		}
		
		@Override
		public void setValue(Object value) {
			spinner.getValueFactory().setValue((Integer) value);
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
		public void setValue(Object value) {
			setSelected((boolean) value);
		}

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
