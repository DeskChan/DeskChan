package info.deskchan.core;

public interface PluginLoader {
	
	boolean matchPath(Path path);
	
	void loadByPath(Path path) throws Throwable;
	
}
