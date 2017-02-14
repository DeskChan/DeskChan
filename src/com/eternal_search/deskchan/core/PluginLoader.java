package com.eternal_search.deskchan.core;

import java.nio.file.Path;

public interface PluginLoader {
	
	boolean matchPath(Path path);
	
	boolean loadByPath(Path path);
	
}
