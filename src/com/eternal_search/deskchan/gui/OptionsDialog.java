package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.Utils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
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
			Object selectValue = skinList.getSelectedValue();
			if (selectValue != null) {
				SkinInfo skinInfo = (SkinInfo) selectValue;
				mainWindow.getCharacterWidget().loadImage(skinInfo.path);
				mainWindow.setDefaultLocation();
			}
		}
	};
	private DefaultMutableTreeNode alternativesTreeRoot;
	private JTree alternativesTree;
	
	OptionsDialog(MainWindow mainWindow) {
		super("deskchan Options");
		this.mainWindow = mainWindow;
		setMinimumSize(new Dimension(400, 300));
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setContentPane(tabbedPane);
		JPanel appearanceTab = new JPanel(new BorderLayout());
		skinList = new JList(loadSkinList().toArray());
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
		JButton button = new JButton(selectSkinAction);
		appearanceTab.add(button, BorderLayout.PAGE_END);
		tabbedPane.addTab("Appearance", appearanceTab);
		JPanel pluginsTab = new JPanel(new BorderLayout());
		DefaultListModel pluginsListModel = new DefaultListModel();
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
			pluginsListModel.addElement(data.toString());
		});
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			for (int i = 0; i < pluginsListModel.size(); ++i) {
				if (pluginsListModel.elementAt(i).equals(data)) {
					pluginsListModel.remove(i);
					break;
				}
			}
		});
		JList pluginsList = new JList(pluginsListModel);
		JScrollPane pluginsListScrollPane = new JScrollPane(pluginsList);
		pluginsTab.add(pluginsListScrollPane);
		tabbedPane.addTab("Plugins", pluginsTab);
		JPanel alternativesTab = new JPanel(new BorderLayout());
		alternativesTreeRoot = new DefaultMutableTreeNode("Alternatives");
		alternativesTree = new JTree(alternativesTreeRoot);
		alternativesTree.setRootVisible(false);
		JScrollPane alternativesScrollPane = new JScrollPane(alternativesTree);
		alternativesTab.add(alternativesScrollPane);
		tabbedPane.addTab("Alternatives", alternativesTab);
		JPanel debugTab = new JPanel(new BorderLayout());
		//
		tabbedPane.addTab("Debug", debugTab);
		pack();
	}
	
	private ArrayList<SkinInfo> loadSkinList() {
		ArrayList<SkinInfo> list = new ArrayList<>();
		Path directoryPath = Utils.getResourcePath("characters");
		if (directoryPath != null) {
			try {
				DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath);
				for (Path skinPath : directoryStream) {
					if (!Files.isDirectory(skinPath)) {
						list.add(new SkinInfo(skinPath));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Collections.sort(list);
		return list;
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
	
	class SkinInfo implements Comparable<SkinInfo> {
		
		String name;
		Path path;
		
		SkinInfo(String name, Path path) {
			this.name = name;
			this.path = path;
		}
		
		SkinInfo(Path path) {
			this(path.getFileName().toString(), path);
		}
		
		@Override
		public int compareTo(SkinInfo o) {
			return name.compareTo(o.name);
		}
		
		@Override
		public String toString() {
			return name;
		}
		
	}
	
}
