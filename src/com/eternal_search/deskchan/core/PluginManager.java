package com.eternal_search.deskchan.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PluginManager {
	
	private static final PluginManager instance = new PluginManager();
	private final Map<String, PluginProxy> plugins = new HashMap<>();
	private final Map<String, Set<MessageListener>> messageListeners = new HashMap<>();
	private final List<PluginLoader> loaders = new ArrayList<>();
	private final Set<String> blacklistedPlugins = new HashSet<>();
	
	private PluginManager() {
	}
	
	public static PluginManager getInstance() {
		return instance;
	}
	
	void initialize() {
		loadPluginByClass(CorePlugin.class);
		try {
			BufferedReader reader = Files.newBufferedReader(getDataDir().resolve("blacklisted-plugins.txt"),
					Charset.forName("UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.length() == 0) continue;
				blacklistedPlugins.add(line);
			}
			reader.close();
		} catch (IOException e) {
			// Do nothing
		}
	}
	
	public boolean initializePlugin(String id, Plugin plugin) {
		if (blacklistedPlugins.contains(id)) {
			return false;
		}
		if (!plugins.containsKey(id)) {
			PluginProxy pluginProxy = new PluginProxy(plugin);
			if (pluginProxy.initialize(id)) {
				plugins.put(id, pluginProxy);
				System.err.println("Registered plugin: " + id);
				sendMessage("core", "core-events:plugin-load", id);
				return true;
			}
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
	
	public synchronized void registerPluginLoader(PluginLoader loader) {
		loaders.add(loader);
	}
	
	public synchronized void unregisterPluginLoader(PluginLoader loader) {
		loaders.remove(loader);
	}
	
	private PluginProxy getPlugin(String name) {
		return plugins.getOrDefault(name, null);
	}
	
	public boolean unloadPlugin(String name) {
		PluginProxy plugin = getPlugin(name);
		if (plugin != null) {
			plugin.unload();
			return true;
		}
		return false;
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
	
	public synchronized void loadPluginByPath(Path path) throws Throwable {
		for (PluginLoader loader : loaders) {
			if (loader.matchPath(path)) {
				loader.loadByPath(path);
				return;
			}
		}
		throw new Exception("Could not match loader for plugin " + path.toString());
	}
	
	public boolean tryLoadPluginByPath(Path path) {
		try {
			loadPluginByPath(path);
			return true;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void loadPluginByName(String name) throws Throwable {
		Path jarPath = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		Path pluginsDirPath;
		if (Files.isDirectory(jarPath)) {
			pluginsDirPath = jarPath.resolve("../../../plugins");
		} else {
			pluginsDirPath = jarPath.getParent().resolve("../plugins");
		}
		loadPluginByPath(pluginsDirPath.resolve(name));
	}
	
	public boolean tryLoadPluginByName(String name) {
		try {
			loadPluginByName(name);
			return true;
		} catch (Throwable e) {
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
		try {
			BufferedWriter writer = Files.newBufferedWriter(getDataDir().resolve("blacklisted-plugins.txt"),
					Charset.forName("UTF-8"));
			for (String id : blacklistedPlugins) {
				writer.write(id);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public Set<String> getBlacklistedPlugins() {
		return blacklistedPlugins;
	}
	
	static Path getDataDir() {
		try {
			Path jarPath = Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			Path path;
			if (Files.isDirectory(jarPath)) {
				path = jarPath.resolve("../../data");
			} else {
				path = jarPath.getParent().resolve("../data");
			}
			return path;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static Path getDataDir(String id) {
		final Path baseDir = getDataDir();
		final Path dataDir = baseDir.resolve(id);
		if (!Files.isDirectory(dataDir)) {
			dataDir.toFile().mkdirs();
			System.err.println("Created directory: " + dataDir.toString());
		}
		return dataDir;
	}
	
}
