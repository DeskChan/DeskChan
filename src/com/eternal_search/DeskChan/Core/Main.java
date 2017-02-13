package com.eternal_search.DeskChan.Core;

public class Main {
	public static void main(String[] args) {
		PluginManager pluginManager = new PluginManager();
		pluginManager.loadPluginByPackageName("com.eternal_search.DeskChan.Gui");
	}
}
