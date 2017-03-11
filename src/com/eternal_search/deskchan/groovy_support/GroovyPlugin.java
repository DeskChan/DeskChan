package com.eternal_search.deskchan.groovy_support;

import com.eternal_search.deskchan.core.MessageListener;
import com.eternal_search.deskchan.core.Plugin;
import com.eternal_search.deskchan.core.PluginProxy;
import com.eternal_search.deskchan.core.ResponseListener;
import groovy.lang.Script;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class GroovyPlugin extends Script implements Plugin {
	
	private PluginProxy pluginProxy = null;
	private List<Runnable> cleanupHandlers = new ArrayList<>();
	private Path pluginDir;
	
	@Override
	public boolean initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		try {
			run();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public void unload() {
		for (Runnable runnable : cleanupHandlers) {
			runnable.run();
		}
	}
	
	protected void sendMessage(String tag, Object data) {
		pluginProxy.sendMessage(tag, data);
	}
	
	protected void sendMessage(String tag, Object data, ResponseListener responseListener) {
		pluginProxy.sendMessage(tag, data, responseListener);
	}
	
	protected void addMessageListener(String tag, MessageListener listener) {
		pluginProxy.addMessageListener(tag, listener);
	}
	
	protected void removeMessageListener(String tag, MessageListener listener) {
		pluginProxy.removeMessageListener(tag, listener);
	}
	
	protected void addCleanupHandler(Runnable handler) {
		cleanupHandlers.add(handler);
	}
	
	protected Path getDataDir() {
		return pluginProxy.getDataDir();
	}
	
	protected Path getPluginDir() {
		return pluginDir;
	}
	
	void setPluginDir(Path path) {
		pluginDir = path;
	}
	
}
