package info.deskchan.gui_javafx;

import info.deskchan.core.Path;
import info.deskchan.gui_javafx.skins.Skin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class AssetsManagerDialog extends FilesManagerDialog {

	private Path folder;
	private int filesFolderStrLength;
	class FileItem extends File {
		FileItem(String init){ super(init); }
		@Override
		public String toString(){
			if (getAbsolutePath().startsWith(folder.getAbsolutePath()))
				return getAbsolutePath().substring(filesFolderStrLength);
			return getAbsolutePath();
		}
	}

	private List<String> acceptableExtensions;
	private List<FileItem> selected;
	private String type;
	private BorderPane pane;
	private FlowPane bottom;


	AssetsManagerDialog(Window parent, String assetsType) {
		super(parent, new ArrayList<>());

		setId("assets-manager-"+assetsType);
		setTitle(Main.getString("assets-manager")+": "+assetsType);

		bottom = new FlowPane();

		pane = new BorderPane();
		pane.setCenter(filesList);
		pane.setBottom(bottom);

		getDialogPane().setContent(pane);

		type = assetsType;

		folder = Main.getPluginProxy().getAssetsDirPath().resolve(assetsType);
		bottom.getChildren().add(
				new PluginOptionsControlItem.HyperlinkItem(
						folder.getAbsolutePath(),
						Main.getString("open-folder")).getNode()
		);

		filesFolderStrLength = folder.getAbsolutePath().length() + 1;

		ObservableList<String> newFiles = FXCollections.observableArrayList();
		for (String file : filesList.getItems())
			newFiles.add(file.substring(filesFolderStrLength));

		filesList.setItems(newFiles);

	}

	@Override
	public List<String> getSelectedFiles(){
		List<String> list = new LinkedList<>();
		for(String file : filesList.getSelectionModel().getSelectedItems())
			if (file != null)
				list.add(folder.resolve(file).getAbsolutePath());
		return list;
	}

	void setAcceptedExtensions(List<String> extensions){
		acceptableExtensions = extensions;
	}

	void setSelected(List<String> files){
		selected = new ArrayList<>();
		if (files == null) return;
		for (String a : files)
			selected.add(new FileItem(a));
	}

	void setURL(String url){
		if (url == null) {
			bottom.getChildren().clear();
			bottom.getChildren().add(
					new PluginOptionsControlItem.HyperlinkItem(
							folder.getAbsolutePath(),
							Main.getString("open-folder")).getNode()
			);
		} else {
			bottom.getChildren().add(
					new PluginOptionsControlItem.HyperlinkItem(url, Main.getString("more")+"...").getNode()
			);
		}

	}

	public void showDialog(){
		List<String> files;

		switch (type){
			case "skins": files = Skin.getSkinList(); break;
			default: files = getFilesList(Main.getPluginProxy().getAssetsDirPath().resolve(type)); break;
		}

		for (String file : files) {
			FileItem item = new FileItem(file);
			filesList.getItems().add(item.toString());
		}


		for (FileItem item : selected) {
			if (!filesList.getItems().contains(item.toString()))
				filesList.getItems().add(item.toString());
			filesList.getSelectionModel().select(item.toString());
		}

		super.showAndWait();
	}
	
	private List<String> getFilesList(Path path) {
		List<String> list = new ArrayList<>();

		for (Path skinPath : path.files()) {
			if (skinPath.isDirectory()) {
				list.addAll(getFilesList(skinPath));
				continue;
			}
			String name = skinPath.toString();
			if (!name.endsWith(".config") && (acceptableExtensions == null || acceptableExtensions.size() == 0)){
				list.add(name);
				continue;
			}
			if (acceptableExtensions != null)
				for (String ext : acceptableExtensions){
				if (name.endsWith(ext)) {
					list.add(name);
					break;
				}
			}
		}

		return list;
	}
	
}
