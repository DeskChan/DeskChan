package info.deskchan.gui_javafx.skins;

import info.deskchan.gui_javafx.Main;
import javafx.geometry.Point2D;

import java.io.File;
import java.io.IOException;
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
			{"scared", "shocked", "tired", "surprised"},
			{"love", "bounty"},
			{"waiting", "normal"},
			{"mad", "grin", "rage", "scared"}
	};
	private final Path path;
	private final String skinName;
	private final Map<String, List<File>> images = new HashMap<>();
	private final Path propertiesPath;
	private final Properties properties = new Properties();
	
	ImageSetSkin(Path path) {
		this.path = path;
		skinName = Skin.getSkinsPath().relativize(path).toString();

		propertiesPath = Main.getPluginProxy().getDataDirPath().resolve(
				"skin_" + skinName + ".properties"
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
		return skinName;
	}

	private List<File> getImageArray(String name) {
		List<File> l = images.getOrDefault(name, null);
		if (l != null) {
			return l;
		}
		l = new ArrayList<>();
		Path imagePath = path.resolve(name);
		if (Files.isDirectory(imagePath)) {
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(imagePath)) {
				for (Path imgPath : directoryStream) {
					l.add(imgPath.toFile());
				}
			} catch (IOException e) {
				Main.getPluginProxy().log(e);
			}
		} else {
			File[] files = path.toFile().listFiles();
			if (files == null) return l;

			for (File file : files){
				String fileName = file.getName(); int index = fileName.indexOf('.');
				if (index >= 0) fileName = fileName.substring(0, index);

				if (fileName.equals(name)){
					l.add(file);
				}
			}
		}
		images.put(name, l);
		return l;
	}
	
	@Override
	public File getImage(String name) {
		List<File> l = getImageArray(name);
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
			if (!propertiesPath.toFile().exists())
				propertiesPath.toFile().createNewFile();
			properties.store(Files.newBufferedWriter(propertiesPath), "Skin properties");
		} catch (Throwable e) {
			Main.log(e);
		}
	}
	
	@Override
	public String toString() {
		/*String name = Skin.getSkinsPath().relativize(path).toString();
		name=name.replace(".pack","");
		return name.substring(0, name.length() - 10) + " [IMAGE SET]";*/
		return path.toFile().getAbsolutePath();
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
