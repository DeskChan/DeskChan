package info.deskchan.gui_javafx;

import info.deskchan.core.PluginManager;
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
	
	static Path getSkinsPath() {
		return PluginManager.getPluginsDirPath().getParent().resolve("skins");
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
	
	static List<String> getSkinList() {
		List<String> list = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(getSkinsPath())) {
			for (Path skinPath : directoryStream) {
				synchronized (App.skinLoaders) {
					for (SkinLoader loader : App.skinLoaders) {
						if (loader.matchByPath(skinPath)) {
							list.add(skinPath.getFileName().toString());
							break;
						}
					}
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
