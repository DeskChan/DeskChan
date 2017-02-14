package com.eternal_search.deskchan.core;

public class CorePlugin implements Plugin {
	
	private PluginProxy pluginProxy = null;
	
	@Override
	public boolean initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		pluginProxy.addMessageListener("core:quit", ((sender, tag, data) -> {
			System.err.println("Plugin " + sender + "requested application quit");
			PluginManager.getInstance().quit();
		}));
		return true;
	}
	
}
