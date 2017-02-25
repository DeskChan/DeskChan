package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkinManagerDialog extends JDialog {
	
	private final MainWindow mainWindow;
	private final JList skinList;
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
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
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
	
	SkinManagerDialog(MainWindow mainWindow, JFrame frame) {
		super(frame, "DeskChan Skin manager", ModalityType.DOCUMENT_MODAL);
		this.mainWindow = mainWindow;
		setLocationByPlatform(true);
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
		add(skinListScrollPane);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(new JButton(selectSkinAction));
		buttonPanel.add(new JButton(addSkinAction));
		buttonPanel.add(new JButton(removeSkinAction));
		add(buttonPanel, BorderLayout.PAGE_END);
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
	
}
