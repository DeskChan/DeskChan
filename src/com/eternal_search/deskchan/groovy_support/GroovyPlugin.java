package com.eternal_search.deskchan.groovy_support;

import com.eternal_search.deskchan.core.Plugin;
import com.eternal_search.deskchan.core.PluginProxy;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GroovyPlugin implements Plugin {
	
	private PluginProxy pluginProxy = null;
	private final Path scriptPath;
	
	GroovyPlugin(Path scriptPath) {
		this.scriptPath = scriptPath;
	}
	
	@Override
	public boolean initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setScriptBaseClass("com.eternal_search.deskchan.groovy_support.GroovyPlugin");
		GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
		try {
			groovyShell.evaluate(Files.newBufferedReader(scriptPath));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public void unload() {
	}
	
	protected void sendMessage(String tag, Object data) {
		pluginProxy.sendMessage(tag, data);
	}
	
}
