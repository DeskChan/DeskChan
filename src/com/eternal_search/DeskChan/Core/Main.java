package com.eternal_search.DeskChan.Core;

public class Main {
	public static void main(String[] args) {
		PluginManager pluginManager = PluginManager.getInstance();
		pluginManager.initialize();
		pluginManager.loadPluginByPackageName("com.eternal_search.DeskChan.GroovySupport");
		pluginManager.loadPluginByPackageName("com.eternal_search.DeskChan.Gui");
	}
}
