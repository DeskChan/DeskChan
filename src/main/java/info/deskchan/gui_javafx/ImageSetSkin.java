package info.deskchan.gui_javafx;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class ImageSetSkin implements Skin {
	private final String[][] replacing = {
			{"smile", "happy", "laugh", "love", "bounty"},
			{"vulgar", "confident", "excitement", "grin"},
			{"angry", "serious", "rage", "cry", "sad"},
			{"sceptic", "facepalm", "despair", "disgusted"},
			{"shy", "sorry", "tired"},
			{"scared", "shocked", "tired"},
			{"love", "bounty"},
			{"waiting", "normal"}
	};
	private final Path path;
	private final Map<String, List<Image>> images = new HashMap<>();
	private final Path propertiesPath;
	private final Properties properties = new Properties();
	
	ImageSetSkin(Path path) {
		this.path = path;
		propertiesPath = Main.getInstance().getPluginProxy().getDataDirPath().resolve(
				"skin_" + getName() + ".properties"
		);
		try {
			properties.load(Files.newBufferedReader(propertiesPath));
		} catch (Throwable e) {
			try {
				properties.load(Files.newBufferedReader(path.resolve("properties.txt")));
			} catch (Throwable e2) {
				// Do nothing
			}
		}
	}
	
	@Override
	public String getName() {
		return Skin.getSkinsPath().relativize(path).toString();
	}
	
	private List<Image> getImageArray(String name) {
		List<Image> l = images.getOrDefault(name, null);
		if (l != null) {
			return l;
		}
		l = new ArrayList<>();
		Path imagePath = path.resolve(name);
		if (Files.isDirectory(imagePath)) {
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(imagePath)) {
				for (Path imgPath : directoryStream) {
					try (InputStream inputStream = Files.newInputStream(imgPath)) {
						l.add(new Image(inputStream));
					} catch (IOException e) {
						Main.getInstance().getPluginProxy().log(e);
					}
				}
			} catch (IOException e) {
				Main.getInstance().getPluginProxy().log(e);
			}
		} else {
			imagePath = imagePath.resolveSibling(imagePath.getFileName() + ".png");
			if (Files.isReadable(imagePath)) {
				try (InputStream inputStream = Files.newInputStream(imagePath)) {
					l.add(new Image(inputStream));
				} catch (IOException e) {
					Main.getInstance().getPluginProxy().log(e);
				}
			}
		}
		images.put(name, l);
		return l;
	}
	
	@Override
	public Image getImage(String name) {
		List<Image> l = getImageArray(name);
		if (l.size() == 0) {
			for (String[] aReplacing : replacing) {
				for (int j = 0; j < aReplacing.length; j++) {
					if (aReplacing[j].equals(name)) {
						for (int k = 0; k < aReplacing.length; k++) {
							if (k == j) {
								continue;
							}
							l = getImageArray(aReplacing[k]);
							if (l.size() != 0) {
								return l.get(ThreadLocalRandom.current().nextInt(0, l.size()));
							}
						}
					}
				}
			}
			if (name.equals("normal")) {
				return null;
			} else {
				return getImage("normal");
			}
		}
		int i = ThreadLocalRandom.current().nextInt(0, l.size());
		return l.get(i);
	}
	
	@Override
	public Point2D getPreferredBalloonPosition(String imageName) {
		try {
			String key = "balloon_offset." + imageName;
			String value = properties.getProperty(key, null);
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
			String key = "balloon_offset." + imageName;
			String value = String.valueOf(position.getX()) + ";" + String.valueOf(position.getY());
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
		String name = Skin.getSkinsPath().relativize(path).toString();
		name=name.replace(".pack","");
		return name.substring(0, name.length() - 10) + " [IMAGE SET]";
	}
	
	static class Loader implements SkinLoader {
		
		@Override
		public boolean matchByPath(Path path) {
			return Files.isDirectory(path) &&
					path.getFileName().toString().endsWith(".image_set");
		}
		
		@Override
		public Skin loadByPath(Path path) {
			return new ImageSetSkin(path);
		}
		
	}
	
}
