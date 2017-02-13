package com.eternal_search.DeskChan.Gui;

import javax.swing.*;
import java.awt.*;

public class OptionsDialog extends JFrame {
	
	private MainWindow mainWindow;
	private JTabbedPane tabbedPane = new JTabbedPane();
	private JList skinList;
	
	OptionsDialog(MainWindow mainWindow) {
		super("DeskChan Options");
		this.mainWindow = mainWindow;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setContentPane(tabbedPane);
		JPanel appearanceTab = new JPanel();
		skinList.setLayoutOrientation(JList.VERTICAL);
		appearanceTab.add(skinList);
		tabbedPane.addTab("Appearance", appearanceTab);
		pack();
	}
	
	@Override
	public void dispose() {
		mainWindow.optionsDialog = null;
		super.dispose();
	}
	
}
