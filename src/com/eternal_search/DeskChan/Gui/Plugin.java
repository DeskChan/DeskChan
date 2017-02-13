package com.eternal_search.DeskChan.Gui;

import com.eternal_search.DeskChan.Core.PluginManager;

import java.util.Map;

public class Plugin extends com.eternal_search.DeskChan.Core.Plugin {
	
	public Plugin(PluginManager pluginManager) {
		super("gui", pluginManager);
		MainWindow.createAndShowGUI();
	}
	
	@Override
	public void handleMessage(String sender, String tag, Object data) {
		super.handleMessage(sender, tag, data);
		MainWindow mainWindow = MainWindow.getInstance();
		if (tag.equals("gui:say")) {
			if (data instanceof Map) {
				Map m = (Map) data;
				Object text = m.getOrDefault("text", null);
				if (text == null) {
					mainWindow.showBalloon((String) null);
				} else {
					mainWindow.showBalloon(text.toString());
				}
			}
		}
	}
	
}
