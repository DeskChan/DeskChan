package info.deskchan.gui_javafx;

import info.deskchan.core.PluginManager;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public interface Skin {
	
	String getName();
	
	Image getImage(String name);
	
	Point2D getPreferredBalloonPosition(String imageName);
	
	void overridePreferredBalloonPosition(String imageName, Point2D position);
	
	static Path getSkinsPath() {
		Path path = PluginManager.getPluginsDirPath().getParent().resolve("skins");
		if (!Files.isDirectory(path)) {
			path = PluginManager.getPluginsDirPath().getParent().resolve("data").resolve("skins");
		}
		return path;
	}
	
	static Path getSkinPath(String name) {
		return getSkinsPath().resolve(name);
	}
	
	static Skin load(Path path) {
		synchronized (App.skinLoaders) {
			for (SkinLoader loader : App.skinLoaders) {
				if (loader.matchByPath(path)) {
					return loader.loadByPath(path);
				}
			}
		}
		return null;
	}
	
	static Skin load(String name) {
		if (name == null) {
			return null;
		}
		synchronized (App.skinLoaders) {
			for (SkinLoader loader : App.skinLoaders) {
				Skin skin = loader.loadByName(name);
				if (skin != null) {
					return skin;
				}
			}
		}
		return load(getSkinPath(name));
	}
	static List<String> getSkinList(Path path){
		List<String> list = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			for (Path skinPath : directoryStream) {
				boolean foundLoader=false;
				synchronized (App.skinLoaders) {
					for (SkinLoader loader : App.skinLoaders) {
						if (loader.matchByPath(skinPath)) {
							list.add(skinPath.toString());
							foundLoader=true;
							break;
						}
					}
				}
				if(!foundLoader && Files.isDirectory(skinPath)){
					list.addAll(getSkinList(skinPath));
				}
			}
			synchronized (App.skinLoaders) {
				for (SkinLoader loader : App.skinLoaders) {
					list.addAll(loader.getNames());
				}
			}
		} catch (IOException e) {
			Main.log(e);
		}
		return list;
	}
	static List<String> getSkinList() {
		return getSkinList(getSkinsPath());
	}
	
	static void registerSkinLoader(SkinLoader loader) {
		synchronized (App.skinLoaders) {
			App.skinLoaders.add(loader);
		}
	}
	
	static void unregisterSkinLoader(SkinLoader loader) {
		synchronized (App.skinLoaders) {
			App.skinLoaders.remove(loader);
		}
	}
	
}
