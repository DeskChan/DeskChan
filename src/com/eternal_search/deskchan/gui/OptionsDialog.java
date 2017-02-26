package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.PluginManager;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

class OptionsDialog extends JFrame implements ItemListener {
	
	private final MainWindow mainWindow;
	private final JComboBox cardsComboBox = new JComboBox(new DefaultComboBoxModel());
	private final JPanel cards = new JPanel(new CardLayout());
	private final Action openSkinManagerAction = new AbstractAction("...") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			SkinManagerDialog dialog = new SkinManagerDialog(mainWindow, OptionsDialog.this);
			dialog.setVisible(true);
			openSkinManagerAction.putValue(Action.NAME, mainWindow.getCharacterWidget().getCurrentSkin().getName());
		}
	};
	private final Action changeBalloonFontAction = new AbstractAction("...") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			JDialog dialog = new JDialog(OptionsDialog.this, MainWindow.getString("select_font"),
					JDialog.ModalityType.DOCUMENT_MODAL);
			JFontChooser chooser = new JFontChooser();
			chooser.setSelectedFont(mainWindow.balloonTextFont);
			dialog.add(chooser);
			dialog.setLocationByPlatform(true);
			dialog.pack();
			dialog.setVisible(true);
			mainWindow.balloonTextFont = chooser.getSelectedFont();
			changeBalloonFontAction.putValue(Action.NAME, mainWindow.balloonTextFont.getName() + ", " +
					String.valueOf(mainWindow.balloonTextFont.getSize()));
			MainWindow.properties.setProperty("balloon.font.family", mainWindow.balloonTextFont.getFamily());
			MainWindow.properties.setProperty("balloon.font.size",
					String.valueOf(mainWindow.balloonTextFont.getSize()));
			MainWindow.properties.setProperty("balloon.font.style",
					String.valueOf(mainWindow.balloonTextFont.getStyle()));
		}
	};
	private final JSpinner balloonDefaultTimeoutSpinner = new JSpinner(new SpinnerNumberModel(0,
			0, 600000, 500));
	private JComboBox windowModeComboBox;
	private JList pluginsList;
	private final Action loadPluginAction = new AbstractAction(MainWindow.getString("load")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
			chooser.setDialogTitle(MainWindow.getString("load_plugin"));
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
				Path path = chooser.getSelectedFile().toPath();
				try {
					PluginManager.getInstance().loadPluginByPath(path);
				} catch (Throwable e) {
					MainWindow.showThrowable(OptionsDialog.this, e);
				}
			}
		}
	};
	private final Action unloadPluginAction = new AbstractAction(MainWindow.getString("unload")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			String plugin = pluginsList.getSelectedValue().toString();
			if ((plugin != null) && !plugin.equals("core")) {
				PluginManager.getInstance().unloadPlugin(plugin);
			}
		}
	};
	private DefaultMutableTreeNode alternativesTreeRoot;
	private JTree alternativesTree;
	private JTextField debugTagTextField;
	private JTextArea debugDataTextArea;
	private final Action debugSendMessageAction = new AbstractAction(MainWindow.getString("send")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			String tag = debugTagTextField.getText();
			String dataStr = debugDataTextArea.getText();
			try {
				JSONObject json = new JSONObject(dataStr);
				Object data = json.toMap();
				mainWindow.getPluginProxy().sendMessage(tag, data);
			} catch (Throwable e) {
				MainWindow.showThrowable(OptionsDialog.this, e);
			}
		}
	};
	private List<PluginOptionsTab> pluginsOptionsTabs = new ArrayList<>();
	
	OptionsDialog(MainWindow mainWindow) {
		super(MainWindow.getString("deskchan_options"));
		this.mainWindow = mainWindow;
		setMinimumSize(new Dimension(600, 300));
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		cardsComboBox.addItemListener(this);
		JPanel appearanceTab = new JPanel(new BorderLayout());
		JPanel panel = new JPanel(new GridLayout(5, 2));
		panel.add(new JLabel(MainWindow.getString("look_and_feel")));
		panel.add(new LookAndFeelComboBox());
		panel.add(new JLabel(MainWindow.getString("skin")));
		panel.add(new JButton(openSkinManagerAction));
		panel.add(new JLabel(MainWindow.getString("balloon_font")));
		panel.add(new JButton(changeBalloonFontAction));
		panel.add(new JLabel(MainWindow.getString("balloon_default_timeout")));
		balloonDefaultTimeoutSpinner.addChangeListener((event) -> {
			mainWindow.balloonDefaultTimeout = (int) balloonDefaultTimeoutSpinner.getValue();
			MainWindow.properties.setProperty("balloon.defaultTimeout",
					String.valueOf(mainWindow.balloonDefaultTimeout));
		});
		panel.add(balloonDefaultTimeoutSpinner);
		panel.add(new JLabel(MainWindow.getString("window_mode")));
		windowModeComboBox = new JComboBox<String>(new String[] {
				MainWindow.getString("window_mode.normal"),
				MainWindow.getString("window_mode.top_most")
		});
		windowModeComboBox.setSelectedIndex(mainWindow.getWindowMode());
		windowModeComboBox.addItemListener(itemEvent -> {
			mainWindow.setWindowMode(windowModeComboBox.getSelectedIndex());
		});
		panel.add(windowModeComboBox);
		appearanceTab.add(panel, BorderLayout.PAGE_START);
		addTab(MainWindow.getString("appearance"), appearanceTab);
		JPanel pluginsTab = new JPanel(new BorderLayout());
		DefaultListModel pluginsListModel = new DefaultListModel();
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
			MainWindow.runOnEventThread(() -> {
				pluginsListModel.addElement(data.toString());
			});
		});
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			MainWindow.runOnEventThread(() -> {
				for (int i = 0; i < pluginsListModel.size(); ++i) {
					if (pluginsListModel.elementAt(i).equals(data)) {
						pluginsListModel.remove(i);
						break;
					}
				}
			});
		});
		pluginsList = new JList(pluginsListModel);
		JScrollPane pluginsListScrollPane = new JScrollPane(pluginsList);
		pluginsTab.add(pluginsListScrollPane);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(new JButton(loadPluginAction));
		buttonPanel.add(new JButton(unloadPluginAction));
		pluginsTab.add(buttonPanel, BorderLayout.PAGE_END);
		addTab(MainWindow.getString("plugins"), pluginsTab);
		JPanel alternativesTab = new JPanel(new BorderLayout());
		alternativesTreeRoot = new DefaultMutableTreeNode("Alternatives");
		alternativesTree = new JTree(alternativesTreeRoot);
		alternativesTree.setRootVisible(false);
		JScrollPane alternativesScrollPane = new JScrollPane(alternativesTree);
		alternativesTab.add(alternativesScrollPane);
		addTab(MainWindow.getString("alternatives"), alternativesTab);
		JPanel debugTab = new JPanel(new BorderLayout());
		debugTagTextField = new JTextField("DeskChan:say");
		debugTab.add(debugTagTextField, BorderLayout.PAGE_START);
		debugDataTextArea = new JTextArea("{\n\t\"text\": \"Test\"\n}\n");
		JScrollPane debugDataTextAreaScrollPane = new JScrollPane(debugDataTextArea);
		debugTab.add(debugDataTextAreaScrollPane);
		debugTab.add(new JButton(debugSendMessageAction), BorderLayout.PAGE_END);
		addTab(MainWindow.getString("debug"), debugTab);
		add(cardsComboBox, BorderLayout.PAGE_START);
		add(cards, BorderLayout.CENTER);
		pack();
	}
	
	void updateOptions() {
		openSkinManagerAction.putValue(Action.NAME, mainWindow.getCharacterWidget().getCurrentSkin().getName());
		changeBalloonFontAction.putValue(Action.NAME, mainWindow.balloonTextFont.getName() + ", " +
				String.valueOf(mainWindow.balloonTextFont.getSize()));
		balloonDefaultTimeoutSpinner.setValue(mainWindow.balloonDefaultTimeout);
		mainWindow.getPluginProxy().sendMessage("core:query-alternatives-map", null, (sender, data) -> {
			alternativesTreeRoot.removeAllChildren();
			Map<String, Object> m = (Map<String, Object>) (((Map) data).get("map"));
			for (Map.Entry<String, Object> entry : m.entrySet()) {
				DefaultMutableTreeNode alternativeRoot = new DefaultMutableTreeNode(entry.getKey());
				for (Map<String, Object> info : (List<Map<String, Object>>) (entry.getValue())) {
					String tag = info.get("tag").toString();
					String plugin = info.get("plugin").toString();
					int priority = (int) info.get("priority");
					String text = tag + " (plugin: " + plugin + ", priority: " + String.valueOf(priority) + ")";
					DefaultMutableTreeNode alternativeNode = new DefaultMutableTreeNode(text);
					alternativeRoot.add(alternativeNode);
				}
				alternativesTreeRoot.add(alternativeRoot);
			}
			alternativesTree.expandPath(new TreePath(alternativesTreeRoot.getPath()));
		});
	}
	
	void addTab(String name, JComponent component) {
		cards.add(component, name);
		cardsComboBox.addItem(name);
	}
	
	@Override
	public void itemStateChanged(ItemEvent itemEvent) {
		CardLayout cardLayout = (CardLayout) cards.getLayout();
		cardLayout.show(cards, (String) itemEvent.getItem());
	}
	
	void addTab(String name, String plugin, String msgTag, List controls) {
		PluginOptionsTab tab = new PluginOptionsTab(name, plugin);
		tab.setLayout(new BorderLayout());
		JPanel panel = createControlsFromList(controls, tab.inputs);
		if (msgTag != null) {
			panel.add(new JButton(new AbstractAction(MainWindow.getString("save")) {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					Map<String, Object> data = new HashMap<>();
					for (Map.Entry<String, PluginOptionsControlItem> entry : tab.inputs.entrySet()) {
						String id = entry.getKey();
						PluginOptionsControlItem control = entry.getValue();
						data.put(id, control.getValue());
					}
					mainWindow.getPluginProxy().sendMessage(msgTag, data);
				}
			}));
		}
		tab.add(panel, BorderLayout.PAGE_START);
		addTab(name, tab);
		pluginsOptionsTabs.add(tab);
	}
	
	void removeTabsByPlugin(String plugin) {
		List<PluginOptionsTab> tabsToRemove = new ArrayList<>();
		for (PluginOptionsTab tab : pluginsOptionsTabs) {
			if (tab.getPlugin().equals(plugin)) {
				tabsToRemove.add(tab);
				cards.remove(tab);
				cardsComboBox.removeItem(tab.getTabName());
			}
		}
		pluginsOptionsTabs.removeAll(tabsToRemove);
	}
	
	JPanel createControlsFromList(List controls, Map<String, PluginOptionsControlItem> inputs) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		for (Object control : controls) {
			Map<String, Object> m = (Map<String, Object>) control;
			panel.add(createControlFromMap(m, inputs));
		}
		return panel;
	}
	
	JComponent createControlFromMap(Map<String, Object> m, Map<String, PluginOptionsControlItem> inputs) {
		String type = (String) m.getOrDefault("type", "invalid");
		PluginOptionsControlItem control = null;
		switch (type) {
			case "Label":
				control = new PluginOptionsControlItem.Label();
				break;
			case "TextField":
				control = new PluginOptionsControlItem.TextField();
				break;
			case "Spinner":
				control = new PluginOptionsControlItem.Spinner();
				break;
			case "ComboBox":
				control = new PluginOptionsControlItem.ComboBox();
				break;
			case "Button":
				control = new PluginOptionsControlItem.Button();
				break;
			default:
				return null;
		}
		control.setMainWindow(mainWindow);
		control.setOptions(m);
		Object value = m.getOrDefault("value", null);
		control.setValue(value);
		String id = (String) m.getOrDefault("id", null);
		if (id != null) {
			inputs.put(id, control);
		}
		String labelStr = (String) m.getOrDefault("label", null);
		if (labelStr != null) {
			JPanel container = new JPanel(new FlowLayout());
			container.add(new JLabel(labelStr + ":"));
			container.add(control.getComponent());
			return container;
		}
		return control.getComponent();
	}
	
	static class PluginOptionsTab extends JPanel {
		
		private final String name;
		private final String plugin;
		private final Map<String, PluginOptionsControlItem> inputs = new HashMap<>();
		
		PluginOptionsTab(String name, String plugin) {
			super();
			this.name = name;
			this.plugin = plugin;
		}
		
		String getTabName() {
			return name;
		}
		
		String getPlugin() {
			return plugin;
		}
		
	}
	
}
