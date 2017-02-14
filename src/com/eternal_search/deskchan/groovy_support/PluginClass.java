package com.eternal_search.deskchan.groovy_support;

import com.eternal_search.deskchan.core.Plugin;
import com.eternal_search.deskchan.core.PluginLoader;
import com.eternal_search.deskchan.core.PluginManager;
import com.eternal_search.deskchan.core.PluginProxy;

import java.nio.file.Path;

public class PluginClass implements Plugin, PluginLoader {
	
	@Override
	public boolean initialize(PluginProxy pluginProxy) {
		PluginManager.getInstance().registerPluginLoader(this);
		return true;
	}
	
	@Override
	public void unload() {
		PluginManager.getInstance().unregisterPluginLoader(this);
	}
	
	@Override
	public boolean matchPath(Path path) {
		return path.endsWith(".groovy");
	}
	
	@Override
	public boolean loadByPath(Path path) {
		GroovyPlugin plugin = new GroovyPlugin(path);
		return PluginManager.getInstance().initializePlugin(path.toString(), plugin);
	}
	
}
