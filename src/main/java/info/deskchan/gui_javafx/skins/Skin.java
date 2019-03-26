package info.deskchan.gui_javafx.skins;

import info.deskchan.core.Path;
import info.deskchan.core.PluginManager;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.panes.sprite_drawers.Sprite;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface Skin {

	static final List<SkinLoader> skinLoaders = Arrays.asList(
			new SingleImageSkin.Loader(), new ImageSetSkin.Loader(), new DaytimeDependentSkin.Loader(), new ImageStackSetSkin.Loader()
	);
	
	String getName();
	
	Sprite getImage(String name);
	
	Point2D getPreferredBalloonPosition(String imageName);
	
	void overridePreferredBalloonPosition(String imageName, Point2D position);
	
	static Path getSkinsPath() {
		Path path = PluginManager.getAssetsDirPath().resolve("skins");
		if (!path.isDirectory()) {
			path = PluginManager.getPluginsDirPath().getParentPath().resolve("assets").resolve("skins");
		}
		return new Path(path.getAbsoluteFile());
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
	
	static List<String> getSkinList(Path path) {
		List<String> list = new ArrayList<>();
		for (Path skinPath : path.files()) {
			if (skinPath.isDirectory() && skinPath.getName().endsWith(".pack")) {
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
