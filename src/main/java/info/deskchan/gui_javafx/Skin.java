package info.deskchan.gui_javafx;

import info.deskchan.core.PluginManager;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

interface Skin {
	
	String getName();
	
	Image getImage(String name);
	
	static Path getSkinsPath() {
		return PluginManager.getPluginsDirPath().getParent().resolve("skins");
	}
	
	static Path getSkinPath(String name) {
		return getSkinsPath().resolve(name);
	}
	
	static Skin load(Path path) {
		if (Files.isDirectory(path)) {
			return new ImageSetSkin(path);
		} else if (Files.isReadable(path) && path.getFileName().toString().endsWith(".png")) {
			return new SingleImageSkin(path);
		}
		return null;
	}
	
	static Skin load(String name) {
		return load(getSkinPath(name));
	}
	
	static List<String> getSkinList() {
		List<String> list = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(getSkinsPath())) {
			for (Path skinPath : directoryStream) {
				list.add(skinPath.getFileName().toString());
			}
		} catch (IOException e) {
			Main.log(e);
		}
		return list;
	}
	
}
