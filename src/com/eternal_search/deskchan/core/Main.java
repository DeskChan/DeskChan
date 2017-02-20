package com.eternal_search.deskchan.core;

import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) {
		PluginManager pluginManager = PluginManager.getInstance();
		pluginManager.initialize();
		pluginManager.loadPluginByPackageName("com.eternal_search.deskchan.groovy_support");
		pluginManager.loadPluginByPackageName("com.eternal_search.deskchan.gui");
		pluginManager.tryLoadPluginByName("random_phrases");
	}
}
