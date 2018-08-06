package info.deskchan.gui_javafx.skins;

import info.deskchan.core.PluginManager;
import info.deskchan.gui_javafx.Main;
import javafx.geometry.Point2D;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface Skin {

	static final List<SkinLoader> skinLoaders = Arrays.asList(
			new SingleImageSkin.Loader(), new ImageSetSkin.Loader(), new DaytimeDependentSkin.Loader()
	);
	
	String getName();
	
	File getImage(String name);
	
	Point2D getPreferredBalloonPosition(String imageName);
	
	void overridePreferredBalloonPosition(String imageName, Point2D position);
	
	static Path getSkinsPath() {
		Path path = PluginManager.getAssetsDirPath().resolve("skins");
		if (!Files.isDirectory(path)) {
			path = PluginManager.getPluginsDirPath().getParent().resolve("assets").resolve("skins");
		}
		return path.toAbsolutePath();
	}
	
	static Path getSkinPath(String name) {
		return getSkinsPath().resolve(name);
	}
	
	static Skin load(Path path) {
		synchronized (skinLoaders) {
			for (SkinLoader loader : skinLoaders) {
				if (loader.matchByPath(path)) {
					return loader.loadByPath(path);
				}
			}
		}
		Main.log("Skin by path \"" + path.toString() + "\" not found in assets folder.");
		return null;
	}
	
	static Skin load(String name) {
		if (name == null) {
			return null;
		}
		synchronized (skinLoaders) {
			for (SkinLoader loader : skinLoaders) {
				Skin skin = loader.loadByName(name);
				if (skin != null) {
					return skin;
				}
			}
		}
		return load(getSkinPath(name));
	}
	
	static List<String> getSkinList(Path path) {
		List<String> list = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			for (Path skinPath : directoryStream) {
				if (Files.isDirectory(skinPath) &&
						skinPath.getFileName().toString().endsWith(".pack")) {
					list.addAll(getSkinList(skinPath));
					continue;
				}
				synchronized (skinLoaders) {
					for (SkinLoader loader : skinLoaders) {
						if (loader.matchByPath(skinPath)) {
							list.add(skinPath.toString());
							break;
						}
					}
				}
			}
			synchronized (skinLoaders) {
				for (SkinLoader loader : skinLoaders) {
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
		synchronized (skinLoaders) {
			skinLoaders.add(loader);
		}
	}
	
	static void unregisterSkinLoader(SkinLoader loader) {
		synchronized (skinLoaders) {
			skinLoaders.remove(loader);
		}
	}
	
}
