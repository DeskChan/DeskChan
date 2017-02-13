package com.eternal_search.DeskChan.Core;

public class Plugin {
	
	private PluginManager pluginManager;
	private String id;
	
	public Plugin(String id, PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		this.id = pluginManager.registerPlugin(this, id);
	}
	
	protected void sendMessage(String tag, Object data) {
		pluginManager.sendMessage(this, tag, data);
	}
	
	protected void subscribe(String tag) {
		pluginManager.subscribe(this, tag);
	}
	
	protected void unsubscribe(String tag) {
		pluginManager.unsubscribe(this, tag);
	}
	
	public void handleMessage(String sender, String tag, Object data) {
	}
	
	public String getId() {
		return id;
	}
	
	protected PluginManager getPluginManager() {
		return pluginManager;
	}
	
}
