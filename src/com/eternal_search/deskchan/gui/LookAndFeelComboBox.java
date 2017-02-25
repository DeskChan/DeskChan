package com.eternal_search.deskchan.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

class LookAndFeelComboBox extends JComboBox implements ItemListener {
	
	LookAndFeelComboBox() {
		super();
		for (UIManager.LookAndFeelInfo look : UIManager.getInstalledLookAndFeels()) {
			ItemInfo item = new ItemInfo(look);
			addItem(item);
			if (UIManager.getLookAndFeel().getName().equals(look.getName())) {
				setSelectedItem(item);
			}
			addItemListener(this);
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent itemEvent) {
		try {
			UIManager.setLookAndFeel(((ItemInfo) getSelectedItem()).info.getClassName());
			for (Frame frame : Frame.getFrames()) {
				SwingUtilities.updateComponentTreeUI(frame);
			}
			MainWindow.properties.setProperty("lookAndFeel.className", UIManager.getLookAndFeel().getClass().getName());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private class ItemInfo {
		
		UIManager.LookAndFeelInfo info;
		
		ItemInfo(UIManager.LookAndFeelInfo info) {
			this.info = info;
		}
		
		@Override
		public String toString() {
			return info.getName();
		}
		
	}
	
}
