package com.eternal_search.DeskChan.Core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

public class PluginManager {
	
	private Map<String, Plugin> plugins = new HashMap<>();
	private Map<String, Set<Plugin>> subscriptions = new HashMap<>();
	
	PluginManager() {
		loadPluginByClass(CorePlugin.class);
	}
	
	String registerPlugin(Plugin plugin, String id) {
		String realId = id;
		int i = 0;
		while (plugins.containsKey(realId)) {
			realId = id + String.valueOf(i);
			i += 1;
		}
		plugins.put(realId, plugin);
		System.err.println("Plugin registered: " + realId);
		sendMessage("core", "core-events:plugin-load", realId);
		return realId;
	}
	
	void unregisterPlugin(Plugin plugin) {
		ArrayList<String> emptyTags = new ArrayList<>();
		for (Map.Entry<String, Set<Plugin>> entry: subscriptions.entrySet()) {
			entry.getValue().remove(plugin);
			if (entry.getValue().size() == 0) {
				emptyTags.add(entry.getKey());
			}
		}
		for (String tag: emptyTags) {
			subscriptions.remove(tag);
		}
		plugins.remove(plugin.getId());
		System.err.println("Plugin unregistered: " + plugin.getId());
		sendMessage("core", "core-events:plugin-unload", plugin.getId());
	}
	
	void subscribe(Plugin plugin, String tag) {
		Set<Plugin> pluginSet = subscriptions.getOrDefault(tag, null);
		if (pluginSet == null) {
			pluginSet = new HashSet<>();
			subscriptions.put(tag, pluginSet);
		}
		pluginSet.add(plugin);
	}
	
	void unsubscribe(Plugin plugin, String tag) {
		Set<Plugin> pluginSet = subscriptions.getOrDefault(tag, null);
		if (pluginSet != null) {
			pluginSet.remove(plugin);
			if (pluginSet.size() == 0) {
				subscriptions.remove(tag);
			}
		}
	}
	
	void sendMessage(Plugin sender, String tag, Object data) {
		sendMessage(sender.getId(), tag, data);
	}
	
	private void sendMessage(String sender, String tag, Object data) {
		Plugin plugin = plugins.getOrDefault(tag, null);
		if (plugin != null) {
			plugin.handleMessage(sender, tag, data);
		}
		Set<Plugin> pluginSet = subscriptions.getOrDefault(tag, null);
		if (pluginSet != null) {
			for (Plugin plugin2: pluginSet) {
				plugin2.handleMessage(sender, tag, data);
			}
		}
	}
	
	Plugin getPlugin(String name) {
		return plugins.getOrDefault(name, null);
	}
	
	public boolean loadPluginByClass(Class cls) {
		try {
			Constructor constructor = cls.getDeclaredConstructor(PluginManager.class);
			Object object = constructor.newInstance(this);
			return object instanceof Plugin;
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean loadPluginByClassName(String className) {
		try {
			Class cls = getClass().getClassLoader().loadClass(className);
			return loadPluginByClass(cls);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean loadPluginByPackageName(String packageName) {
		return loadPluginByClassName(packageName + ".Plugin");
	}
	
	public boolean loadPluginByPath(Path path) {
		return false;
	}
	
}
