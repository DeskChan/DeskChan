package com.eternal_search.deskchan.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PluginManager {
	
	private static final PluginManager instance = new PluginManager();
	private final Map<String, PluginProxy> plugins = new HashMap<>();
	private final Map<String, Set<MessageListener>> messageListeners = new HashMap<>();
	private final List<PluginLoader> loaders = new ArrayList<>();
	
	private PluginManager() {
	}
	
	public static PluginManager getInstance() {
		return instance;
	}
	
	void initialize() {
		loadPluginByClass(CorePlugin.class);
	}
	
	public boolean initializePlugin(String id, Plugin plugin) {
		assert !plugins.containsKey(id);
		PluginProxy pluginProxy = new PluginProxy(plugin);
		if (pluginProxy.initialize(id)) {
			plugins.put(id, pluginProxy);
			System.err.println("Registered plugin: " + id);
			sendMessage("core", "core-events:plugin-load", id);
			return true;
		}
		return false;
	}
	
	void unregisterPlugin(PluginProxy pluginProxy) {
		plugins.remove(pluginProxy.getId());
		System.err.println("Unregistered plugin: " + pluginProxy.getId());
		sendMessage("core", "core-events:plugin-unload", pluginProxy.getId());
	}
	
	void sendMessage(String sender, String tag, Object data) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			for (MessageListener listener : listeners) {
				listener.handleMessage(sender, tag, data);
			}
		}
	}
	
	void registerMessageListener(String tag, MessageListener listener) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners == null) {
			listeners = new HashSet<>();
			messageListeners.put(tag, listeners);
		}
		if (tag.equals("core-events:plugin-load")) {
			for (String id : plugins.keySet()) {
				listener.handleMessage("core", "core-events:plugin-load", id);
			}
		}
		listeners.add(listener);
	}
	
	void unregisterMessageListener(String tag, MessageListener listener) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.size() == 0) {
				messageListeners.remove(tag);
			}
		}
	}
	
	public void registerPluginLoader(PluginLoader loader) {
		loaders.add(loader);
	}
	
	public void unregisterPluginLoader(PluginLoader loader) {
		loaders.remove(loader);
	}
	
	PluginProxy getPlugin(String name) {
		return plugins.getOrDefault(name, null);
	}
	
	public boolean loadPluginByClass(Class cls) {
		try {
			Object plugin = cls.newInstance();
			if (plugin instanceof Plugin) {
				String packageName = cls.getPackage().getName();
				if (packageName.startsWith("com.eternal_search.deskchan.")) {
					packageName = packageName.substring("com.eternal_search.deskchan.".length());
				}
				return initializePlugin(packageName, (Plugin) plugin);
			}
		} catch (IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}
		return false;
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
		return loadPluginByClassName(packageName + ".PluginClass");
	}
	
	public boolean loadPluginByPath(Path path) throws Exception {
		for (PluginLoader loader : loaders) {
			if (loader.matchPath(path)) {
				loader.loadByPath(path);
				return true;
			}
		}
		return false;
	}
	
	public boolean tryLoadPluginByPath(Path path) {
		try {
			return loadPluginByPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean loadPluginByName(String name) throws Exception {
		Path jarPath = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		Path pluginsDirPath;
		if (Files.isDirectory(jarPath)) {
			pluginsDirPath = jarPath.resolve("../../../plugins");
		} else {
			pluginsDirPath = jarPath.getParent().resolve("../plugins");
		}
		if (Files.isDirectory(pluginsDirPath)) {
			return loadPluginByPath(pluginsDirPath.resolve(name));
		}
		return false;
	}
	
	public boolean tryLoadPluginByName(String name) {
		try {
			return loadPluginByName(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	void quit() {
		List<PluginProxy> pluginsToUnload = new ArrayList<>();
		for (Map.Entry<String, PluginProxy> entry : plugins.entrySet()) {
			if (!entry.getKey().equals("core")) {
				pluginsToUnload.add(entry.getValue());
			}
		}
		for (PluginProxy plugin : pluginsToUnload) {
			plugin.unload();
		}
		pluginsToUnload.clear();
		getPlugin("core").unload();
		System.exit(0);
	}
	
}
