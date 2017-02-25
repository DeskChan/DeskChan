package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.PluginManager;
import com.eternal_search.deskchan.core.Utils;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;

class OptionsDialog extends JFrame {
	
	private final MainWindow mainWindow;
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private JList skinList;
	private final Action selectSkinAction = new AbstractAction("Select") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			Object selectedValue = skinList.getSelectedValue();
			if (selectedValue != null) {
				Skin skin = (Skin) selectedValue;
				mainWindow.getCharacterWidget().setSkin(skin);
				mainWindow.setDefaultLocation();
			}
		}
	};
	private final Action addSkinAction = new AbstractAction("Add...") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
			chooser.setDialogTitle("Add skin...");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));
			if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
				Path path = chooser.getSelectedFile().toPath();
				DefaultListModel model = (DefaultListModel) skinList.getModel();
				model.addElement(new Skin(path, false));
				storeSkinList();
			}
		}
	};
	private final Action removeSkinAction = new AbstractAction("Remove") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			Object selectedValue = skinList.getSelectedValue();
			if (selectedValue != null) {
				Skin skin = (Skin) selectedValue;
				if (!skin.isBuiltin()) {
					DefaultListModel model = (DefaultListModel) skinList.getModel();
					model.removeElement(skin);
					storeSkinList();
				}
			}
		}
	};
	private JList pluginsList;
	private final Action loadPluginAction = new AbstractAction("Load...") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
			chooser.setDialogTitle("Load plugin...");
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
	private final Action unloadPluginAction = new AbstractAction("Unload") {
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
	private final Action debugSendMessageAction = new AbstractAction("Send") {
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
	
	OptionsDialog(MainWindow mainWindow) {
		super("DeskChan Options");
		this.mainWindow = mainWindow;
		setMinimumSize(new Dimension(400, 300));
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setContentPane(tabbedPane);
		JPanel appearanceTab = new JPanel(new BorderLayout());
		DefaultListModel skinListModel = new DefaultListModel();
		for (Object skin : loadSkinList()) {
			skinListModel.addElement(skin);
		}
		skinList = new JList(skinListModel);
		skinList.setLayoutOrientation(JList.VERTICAL);
		skinList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectSkinAction.actionPerformed(null);
				}
			}
		});
		JScrollPane skinListScrollPane = new JScrollPane(skinList);
		appearanceTab.add(skinListScrollPane);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(new JButton(selectSkinAction));
		buttonPanel.add(new JButton(addSkinAction));
		buttonPanel.add(new JButton(removeSkinAction));
		appearanceTab.add(buttonPanel, BorderLayout.PAGE_END);
		tabbedPane.addTab("Appearance", appearanceTab);
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
		buttonPanel = new JPanel();
		buttonPanel.add(new JButton(loadPluginAction));
		buttonPanel.add(new JButton(unloadPluginAction));
		pluginsTab.add(buttonPanel, BorderLayout.PAGE_END);
		tabbedPane.addTab("Plugins", pluginsTab);
		JPanel alternativesTab = new JPanel(new BorderLayout());
		alternativesTreeRoot = new DefaultMutableTreeNode("Alternatives");
		alternativesTree = new JTree(alternativesTreeRoot);
		alternativesTree.setRootVisible(false);
		JScrollPane alternativesScrollPane = new JScrollPane(alternativesTree);
		alternativesTab.add(alternativesScrollPane);
		tabbedPane.addTab("Alternatives", alternativesTab);
		JPanel debugTab = new JPanel(new BorderLayout());
		debugTagTextField = new JTextField("DeskChan:say");
		debugTab.add(debugTagTextField, BorderLayout.PAGE_START);
		debugDataTextArea = new JTextArea("{\n\t\"text\": \"Test\"\n}\n");
		JScrollPane debugDataTextAreaScrollPane = new JScrollPane(debugDataTextArea);
		debugTab.add(debugDataTextAreaScrollPane);
		debugTab.add(new JButton(debugSendMessageAction), BorderLayout.PAGE_END);
		tabbedPane.addTab("Debug", debugTab);
		pack();
	}
	
	private ArrayList<Skin> loadSkinList() {
		ArrayList<Skin> list = new ArrayList<>();
		Path directoryPath = Utils.getResourcePath("characters");
		if (directoryPath != null) {
			try {
				DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath);
				for (Path skinPath : directoryStream) {
					list.add(new Skin(skinPath, true));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			List<String> lines = Files.readAllLines(mainWindow.getDataDirPath().resolve("extra_skins.txt"));
			for (String line : lines) {
				if (line.isEmpty()) continue;
				Path skinPath = Paths.get(line);
				list.add(new Skin(skinPath, false));
			}
		} catch (IOException e) {
			// Configuration file not found
		}
		Collections.sort(list);
		return list;
	}
	
	private void storeSkinList() {
		ArrayList<Skin> list = new ArrayList<>();
		for (Object skinInfo : ((DefaultListModel) skinList.getModel()).toArray()) {
			if (skinInfo instanceof Skin) {
				list.add((Skin) skinInfo);
			}
		}
		storeSkinList(list);
	}
	
	private void storeSkinList(ArrayList<Skin> list) {
		try {
			PrintWriter writer = new PrintWriter(mainWindow.getDataDirPath().resolve("extra_skins.txt").toFile());
			for (Skin skin : list) {
				if (skin.isBuiltin()) continue;
				writer.println(skin.getBasePath().toString());
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void updateOptions() {
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
	
}
