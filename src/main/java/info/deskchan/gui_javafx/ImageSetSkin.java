package info.deskchan.gui_javafx;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

class ImageSetSkin implements Skin {
	
	private final Path path;
	private final Map<String, List<Image>> images = new HashMap<>();
	
	ImageSetSkin(Path path) {
		this.path = path;
	}
	
	@Override
	public String getName() {
		return path.getFileName().toString();
	}
	
	@Override
	public Image getImage(String name) {
		List<Image> l = images.getOrDefault(name, null);
		if (l == null) {
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
		}
		if (l.size() == 0) {
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
	public String toString() {
		return path.getFileName().toString() + " [IMAGE SET]";
	}
	
	static class Loader implements SkinLoader {
		
		@Override
		public boolean matchByPath(Path path) {
			return Files.isDirectory(path);
		}
		
		@Override
		public Skin loadByPath(Path path) {
			return new ImageSetSkin(path);
		}
		
	}
	
}
