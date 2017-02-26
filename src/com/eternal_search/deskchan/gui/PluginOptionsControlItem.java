package com.eternal_search.deskchan.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public interface PluginOptionsControlItem {
	
	default void setOptions(Map<String, Object> options) {
	}
	void setValue(Object value);
	Object getValue();
	JComponent getComponent();
	default void setMainWindow(MainWindow mainWindow) {
	}
	
	class Label extends JLabel implements PluginOptionsControlItem {
		
		@Override
		public void setValue(Object value) {
			setText(value.toString());
		}
		
		@Override
		public Object getValue() {
			return getText();
		}
		
		@Override
		public JComponent getComponent() {
			return this;
		}
	}
	
	class TextField extends JTextField implements PluginOptionsControlItem {
		
		@Override
		public void setValue(Object value) {
			setText(value.toString());
		}
		
		@Override
		public Object getValue() {
			return getText();
		}
		
		@Override
		public JComponent getComponent() {
			return this;
		}
		
	}
	
	class ComboBox extends JComboBox<String> implements PluginOptionsControlItem {
		
		@Override
		public void setOptions(Map<String, Object> options) {
			List<String> values = (List<String>) options.get("values");
			for (String value : values) {
				addItem(value);
			}
		}
		
		@Override
		public void setValue(Object value) {
			setSelectedIndex((int) value);
		}
		
		@Override
		public Object getValue() {
			return getSelectedIndex();
		}
		
		@Override
		public JComponent getComponent() {
			return this;
		}
	}
	
	class Spinner extends JSpinner implements PluginOptionsControlItem {
		
		@Override
		public void setOptions(Map<String, Object> options) {
			setModel(new SpinnerNumberModel(
					(int) options.getOrDefault("value", 0),
					(int) options.getOrDefault("min", 0),
					(int) options.getOrDefault("max", 100),
					(int) options.getOrDefault("step", 1)
			));
		}
		
		@Override
		public JComponent getComponent() {
			return this;
		}
		
	}
	
	class Button extends JButton implements PluginOptionsControlItem, ActionListener {
		
		private MainWindow mainWindow;
		private String msgTag;
		private Object msgData;
		
		@Override
		public void setMainWindow(MainWindow mainWindow) {
			this.mainWindow = mainWindow;
			addActionListener(this);
		}
		
		@Override
		public void setOptions(Map<String, Object> options) {
			msgTag = (String) options.getOrDefault("msgTag", null);
			msgData = options.getOrDefault("msgData", null);
		}
		
		@Override
		public void setValue(Object value) {
			setText(value.toString());
		}
		
		@Override
		public String getValue() {
			return getText();
		}
		
		@Override
		public JComponent getComponent() {
			return this;
		}
		
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			if (msgTag != null) {
				mainWindow.getPluginProxy().sendMessage(msgTag, msgData);
			}
		}
	}
	
}
