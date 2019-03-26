package info.deskchan.gui_javafx.skins;

import info.deskchan.core.Path;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.panes.sprite_drawers.ImageSprite;
import info.deskchan.gui_javafx.panes.sprite_drawers.SVGSprite;
import info.deskchan.gui_javafx.panes.sprite_drawers.Sprite;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;

import java.io.File;
import java.util.Properties;

class SingleImageSkin implements Skin {
	
	private final Path path;
	private final Path propertiesPath;
	private final Properties properties = new Properties();
	private final Sprite sprite;
	
	SingleImageSkin(Path path) {
		this.path = path;
		propertiesPath = Main.getPluginProxy().getDataDirPath().resolve(
				"skin_" + getName() + ".properties"
		);
		try {
			properties.load((propertiesPath.newBufferedReader()));
		} catch (Throwable e) {
			try {
				properties.load(path.resolveSibling(path.getName() + ".properties").newBufferedReader());
			} catch (Throwable e2) {
				// Do nothing
			}
		}

		Sprite ts = null;
		try {
			ts = Sprite.getSpriteFromFile(path);
		} catch (Exception e){
			Main.log(e);
			ts = null;
		}
		sprite = ts;
	}
	
	@Override
	public String getName() {
		return Skin.getSkinsPath().relativize(path);
	}
	
	@Override
	public Sprite getImage(String name) {
		return sprite;
	}
	
	@Override
	public Point2D getPreferredBalloonPosition(String imageName) {
		try {
			String value = properties.getProperty("balloon_offset", null);
			if (value == null) {
				return null;
			}
			String[] coords = value.split(";");
			return new Point2D(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
		} catch (Throwable e) {
			return null;
		}
	}
	
	@Override
	public void overridePreferredBalloonPosition(String imageName, Point2D position) {
		try {
			String key = "balloon_offset";
			String value = String.valueOf(position.getX()) + ";" +
					String.valueOf(position.getY());
			String oldValue = properties.getProperty(key);
			if ((oldValue != null) && oldValue.equals(value)) {
				return;
			}
			properties.setProperty(key, value);
			properties.store(propertiesPath.newBufferedWriter(), "Skin properties");
		} catch (Throwable e) {
			Main.log(e);
		}
	}
	
	@Override
	public String toString() {
		/*String name = Skin.getSkinsPath().relativize(path).toString();
		name=name.replace(".pack","");
		return name.substring(0, name.length() - 4) + " [SINGLE IMAGE]";*/
		return path.getAbsolutePath();
	}
	
	static class Loader implements SkinLoader {
		
		@Override
		public boolean matchByPath(Path path) {
			return path.canRead() && path.getName().endsWith(".png");
		}
		
		@Override
		public Skin loadByPath(Path path) {
			return new SingleImageSkin(path);
		}
		
	}
	
}
