package info.deskchan.gui_javafx.skins;

import info.deskchan.gui_javafx.Main;
import javafx.geometry.Point2D;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

class SingleImageSkin implements Skin {
	
	private final File path;
	private final Path propertiesPath;
	private final Properties properties = new Properties();
	
	SingleImageSkin(Path path) {
		this.path = path.toFile();
		propertiesPath = Main.getPluginProxy().getDataDirPath().resolve(
				"skin_" + getName() + ".properties"
		);
		try {
			properties.load(Files.newBufferedReader(propertiesPath));
		} catch (Throwable e) {
			try {
				properties.load(Files.newBufferedReader(
						path.resolveSibling(path.getFileName().toString() + ".properties")
				));
			} catch (Throwable e2) {
				// Do nothing
			}
		}
	}
	
	@Override
	public String getName() {
		return Skin.getSkinsPath().relativize(path.toPath()).toString();
	}
	
	@Override
	public File getImage(String name) {
		return path;
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
			properties.store(Files.newBufferedWriter(propertiesPath), "Skin properties");
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
			return Files.isReadable(path) && path.getFileName().toString().endsWith(".png");
		}
		
		@Override
		public Skin loadByPath(Path path) {
			return new SingleImageSkin(path);
		}
		
	}
	
}
