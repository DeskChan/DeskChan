package info.deskchan.gui_javafx.skins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public interface SkinLoader {
	
	boolean matchByPath(Path path);
	
	Skin loadByPath(Path path);
	
	default List<String> getNames() {
		return new ArrayList<>();
	}
	
	default Skin loadByName(String name) {
		return null;
	}
	
}
