package com.eternal_search.DeskChan.GroovySupport;

import com.eternal_search.DeskChan.Core.Plugin;
import com.eternal_search.DeskChan.Core.PluginLoader;
import com.eternal_search.DeskChan.Core.PluginManager;
import com.eternal_search.DeskChan.Core.PluginProxy;

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
		return false;
	}
	
}
