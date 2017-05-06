package info.deskchan.core;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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
	private String[] args;
	private static OutputStream logStream = null;
	
	/* Singleton */
	
	private PluginManager() {
	}
	
	public static PluginManager getInstance() {
		return instance;
	}
	
	/* Core initialization */
	
	void initialize(String[] args) {
		this.args = args;
		try {
			logStream = Files.newOutputStream(getDataDirPath().resolve("DeskChan.log"));
		} catch (IOException e) {
			log(e);
		}
		CoreInfo.printInfo();
		tryLoadPluginByClass(CorePlugin.class);
		loadPluginsBlacklist();
	}
	
	public String[] getArgs() {
		return args;
	}
	
	/* Plugin initialization and unloading */
	
	public boolean initializePlugin(String id, Plugin plugin) {
		if (blacklistedPlugins.contains(id)) {
			return false;
		}
		if (!plugins.containsKey(id)) {
			PluginProxy pluginProxy = new PluginProxy(plugin);
			if (pluginProxy.initialize(id)) {
				plugins.put(id, pluginProxy);
				log("Registered plugin: " + id);
				sendMessage("core", "core-events:plugin-load", id);
				return true;
			}
		}
		return false;
	}
	
	void unregisterPlugin(PluginProxy pluginProxy) {
		plugins.remove(pluginProxy.getId());
		log("Unregistered plugin: " + pluginProxy.getId());
		sendMessage("core", "core-events:plugin-unload", pluginProxy.getId());
	}
	
	public boolean unloadPlugin(String name) {
		PluginProxy plugin = plugins.getOrDefault(name, null);
		if (plugin != null) {
			plugin.unload();
			return true;
		}
		return false;
	}
	
	/* Message bus */
	
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
	
	void sendMessage(String sender, String tag, Object data) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			for (MessageListener listener : listeners) {
				listener.handleMessage(sender, tag, data);
			}
		}
	}
	
	/* Plugin loaders */
	
	public synchronized void registerPluginLoader(PluginLoader loader) {
		loaders.add(loader);
	}
	
	public synchronized void unregisterPluginLoader(PluginLoader loader) {
		loaders.remove(loader);
	}
	
	public boolean loadPluginByClass(Class cls) throws Throwable {
		Object plugin = cls.newInstance();
		if (plugin instanceof Plugin) {
			String packageName = cls.getPackage().getName();
			if (packageName.startsWith("info.deskchan.")) {
				packageName = packageName.substring("info.deskchan.".length());
			}
			return initializePlugin(packageName, (Plugin) plugin);
		}
		return false;
	}
	
	public boolean tryLoadPluginByClass(Class cls) {
		try {
			return loadPluginByClass(cls);
		} catch (Throwable e) {
			return false;
		}
	}
	
	public boolean loadPluginByClassName(String className) throws Throwable {
		Class cls = getClass().getClassLoader().loadClass(className);
		return loadPluginByClass(cls);
	}
	
	public boolean tryLoadPluginByClassName(String className) {
		try {
			return loadPluginByClassName(className);
		} catch (Throwable e) {
			return false;
		}
	}
	
	public boolean loadPluginByPackageName(String packageName) throws Throwable {
		return loadPluginByClassName(packageName + ".Main");
	}
	
	public boolean tryLoadPluginByPackageName(String packageName) {
		try {
			return loadPluginByPackageName(packageName);
		} catch (Throwable e) {
			return false;
		}
	}
	
	public synchronized boolean loadPluginByPath(Path path) throws Throwable {
		if (Files.isDirectory(path)) {
			Path manifestPath = path.resolve("manifest.json");
			if (Files.isReadable(manifestPath)) {
				try (final InputStream manifestInputStream = Files.newInputStream(manifestPath)) {
					final String manifestStr = IOUtils.toString(manifestInputStream, "UTF-8");
					manifestInputStream.close();
					final JSONObject manifest = new JSONObject(manifestStr);
					if (manifest.has("deps")) {
						final JSONArray deps = manifest.getJSONArray("deps");
						for (Object dep : deps) {
							if (dep instanceof String) {
								String depID = dep.toString();
								if (!tryLoadPluginByName(depID)) {
									throw new Exception("Failed to load dependency " + depID +
											" of plugin " + path.toString());
								}
							}
						}
					}
				} catch (IOException | JSONException e) {
					e.printStackTrace();
				}
			}
		}
		for (PluginLoader loader : loaders) {
			if (loader.matchPath(path)) {
				loader.loadByPath(path);
				return true;
			}
		}
		throw new Exception("Could not match loader for plugin " + path.toString());
	}
	
	public boolean tryLoadPluginByPath(Path path) {
		try {
			return loadPluginByPath(path);
		} catch (Throwable e) {
			log(e);
		}
		return false;
	}
	
	public boolean loadPluginByName(String name) throws Throwable {
		return plugins.containsKey(name) || loadPluginByPath(getPluginDirPath(name));
	}
	
	public boolean tryLoadPluginByName(String name) {
		try {
			return loadPluginByName(name);
		} catch (Throwable e) {
			log(e);
		}
		return false;
	}
	
	/* Application finalization */
	
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
		plugins.get("core").unload();
		savePluginsBlacklist();
		if (logStream != null) {
			try {
				logStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			logStream = null;
		}
		System.exit(0);
	}
	
	/* Plugins blacklist */
	
	public List<String> getBlacklistedPlugins() {
		return new ArrayList<>(blacklistedPlugins);
	}
	
	public void addPluginToBlacklist(String name) {
		if (!name.equals("core")) {
			blacklistedPlugins.add(name);
			unloadPlugin(name);
		}
	}
	
	public void removePluginFromBlacklist(String name) {
		blacklistedPlugins.remove(name);
	}
	
	private void loadPluginsBlacklist() {
		try {
			BufferedReader reader = Files.newBufferedReader(
					getPluginDataDirPath("core").resolve("blacklisted-plugins.txt"),
					Charset.forName("UTF-8")
			);
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.length() == 0) {
					continue;
				}
				blacklistedPlugins.add(line);
			}
			reader.close();
		} catch (IOException e) {
			blacklistedPlugins.add("random_phrases");
		}
	}
	
	private void savePluginsBlacklist() {
		try {
			BufferedWriter writer = Files.newBufferedWriter(
					getPluginDataDirPath("core").resolve("blacklisted-plugins.txt"),
					Charset.forName("UTF-8")
			);
			for (String id : blacklistedPlugins) {
				writer.write(id);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			log(e);
		}
	}
	
	/* Plugins and data directories */
	
	public static Path getCorePath() {
		try {
			return Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			return Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		}
	}
	
	public static Path getPluginsDirPath() {
		Path corePath = getCorePath();
		Path path;
		if (Files.isDirectory(corePath)) {
			path = corePath.resolve("../../../plugins");
		} else {
			path = corePath.getParent().resolve("../plugins");
		}
		return path;
	}
	
	public static Path getPluginDirPath(String name) {
		return getPluginsDirPath().resolve(name);
	}
	
	public static Path getDataDirPath() {
		Path corePath = getCorePath();
		Path path;
		if (Files.isDirectory(corePath)) {
			path = corePath.resolve("../../data");
		} else {
			path = corePath.getParent().resolve("../data");
		}
		if (!Files.isDirectory(path)) {
			path.toFile().mkdir();
			log("Created directory: " + path);
		}
		return path;
	}
	
	public static Path getRootDirPath() {
		Path corePath = getCorePath();
		Path path;
		if (Files.isDirectory(corePath)) {
			path = corePath.resolve("../../");
		} else {
			path = corePath.getParent().resolve("../");
		}
		return path;
	}
	
	public static Path getPluginDataDirPath(String id) {
		final Path baseDir = getDataDirPath();
		final Path dataDir = baseDir.resolve(id);
		if (!Files.isDirectory(dataDir)) {
			dataDir.toFile().mkdirs();
			log("Created directory: " + dataDir.toString());
		}
		return dataDir;
	}
	
	/* Logging */
	
	static void log(String id, String message) {
		String text = id + ": " + message;
		System.err.println(text);
		if (logStream != null) {
			try {
				logStream.write((text + "\n").getBytes("UTF-8"));
			} catch (IOException e) {
				logStream = null;
				log(e);
			}
		}
	}
	
	static void log(String id, Throwable e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		String[] lines = stringWriter.toString().split("\n");
		for (String line : lines) {
			log(id, line);
		}
	}
	
	static void log(String message) {
		log("core", message);
	}
	
	static void log(Throwable e) {
		log("core", e);
	}
	
}
