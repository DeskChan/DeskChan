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
		setMinimumSize(new Dimension(300, 200));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setContentPane(tabbedPane);
		JPanel appearanceTab = new JPanel(new BorderLayout());
		DefaultListModel skinListModel = new DefaultListModel();
		skinList = new JList(skinListModel);
		//skinListModel.addElement("Test");
		skinList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane skinListScrollPane = new JScrollPane(skinList);
		appearanceTab.add(skinListScrollPane);
		tabbedPane.addTab("Appearance", appearanceTab);
		pack();
	}
	
	@Override
	public void dispose() {
		mainWindow.optionsDialog = null;
		super.dispose();
	}
	
}
