package com.eternal_search.deskchan.groovy_support;

import com.eternal_search.deskchan.core.Plugin;
import com.eternal_search.deskchan.core.PluginLoader;
import com.eternal_search.deskchan.core.PluginManager;
import com.eternal_search.deskchan.core.PluginProxy;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
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
		return path.getFileName().toString().endsWith(".groovy");
	}
	
	@Override
	public boolean loadByPath(Path path) {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setScriptBaseClass("com.eternal_search.deskchan.groovy_support.GroovyPlugin");
		GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
		try {
			Script script = groovyShell.parse(path.toFile());
			GroovyPlugin plugin = (GroovyPlugin) script;
			PluginManager.getInstance().initializePlugin(path.toString(), plugin);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
