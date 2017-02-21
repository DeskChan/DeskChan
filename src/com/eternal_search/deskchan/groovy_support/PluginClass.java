package com.eternal_search.deskchan.groovy_support;

import com.eternal_search.deskchan.core.Plugin;
import com.eternal_search.deskchan.core.PluginLoader;
import com.eternal_search.deskchan.core.PluginManager;
import com.eternal_search.deskchan.core.PluginProxy;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
import java.nio.file.Files;
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
		if (Files.isDirectory(path)) {
			path = path.resolve("plugin.groovy");
			if (Files.isReadable(path)) {
				return true;
			}
		}
		return path.getFileName().toString().endsWith(".groovy");
	}
	
	@Override
	public void loadByPath(Path path) throws Throwable {
		String id = path.getFileName().toString();
		if (Files.isDirectory(path)) {
			path = path.resolve("plugin.groovy");
		}
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setScriptBaseClass("com.eternal_search.deskchan.groovy_support.GroovyPlugin");
		GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
		Script script = groovyShell.parse(path.toFile());
		GroovyPlugin plugin = (GroovyPlugin) script;
		PluginManager.getInstance().initializePlugin(id, plugin);
	}
	
}
