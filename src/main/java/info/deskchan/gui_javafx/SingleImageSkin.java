package info.deskchan.gui_javafx;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SingleImageSkin implements Skin {
	
	private final Path path;
	private final Image image;
	
	SingleImageSkin(Path path) {
		this.path = path;
		InputStream stream = null;
		try {
			stream = Files.newInputStream(path);
		} catch (IOException e) {
			Main.getInstance().getPluginProxy().log(e);
		}
		image = (stream != null) ? new Image(stream) : null;
	}
	
	@Override
	public String getName() {
		return path.getFileName().toString();
	}
	
	@Override
	public Image getImage(String name) {
		return image;
	}
	
	@Override
	public String toString() {
		return path.getFileName().toString() + " [SINGLE IMAGE]";
	}
	
}
