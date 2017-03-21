package info.deskchan.core;

import java.nio.file.Path;

public interface PluginLoader {
	
	boolean matchPath(Path path);
	
	void loadByPath(Path path) throws Throwable;
	
}
