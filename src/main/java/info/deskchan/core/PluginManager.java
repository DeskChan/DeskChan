package info.deskchan.core;

import org.apache.commons.io.FilenameUtils;

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

	private static boolean debugBuild = false;
	private static Path corePath = null;
	private static Path pluginsDirPath = null;
	private static Path dataDirPath = null;
	private static Path rootDirPath = null;
	private static Path assetsDirPath = null;
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

	public static boolean isDebugBuild() {
		return debugBuild;
	}

	Set<String> getNamesOfLoadedPlugins() {
		return plugins.keySet();
	}

	/* Plugin initialization and unloading */
	
	public boolean initializePlugin(String id, Plugin plugin, PluginConfig config) throws Throwable {
		if (plugins.containsKey(id)) {
			throw new Throwable("Cannot load plugin " + id + ": plugin with such name already exist");
		}
		if (blacklistedPlugins.contains(id)) {
			return false;
		}
		if (!plugins.containsKey(id)) {
			PluginProxy entity = PluginProxy.Companion.create(plugin, id, config);
			if (entity!=null) {
				plugins.put(id, entity);
				if (config != null) {
					LoaderManager.INSTANCE.registerExtensions(config.getExtensions());
				}
				log("Registered plugin: " + id);
				sendMessage("core", "core-events:plugin-load", id);
				return true;
			}
		}
		return false;
	}

	public boolean initializeInnerPlugin(String id, Plugin plugin) throws Throwable {
		return initializePlugin(id, plugin, PluginConfig.Companion.getInternal().clone());
	}

	void unregisterPlugin(PluginProxyInterface pluginProxy) {
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
	int getMessageListenersCount(String tag) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			return listeners.size();
		}
		return 0;
	}
	void sendMessage(String sender, String tag, Object data) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			for (MessageListener listener : listeners) {
				try {
					listener.handleMessage(sender, tag, data);
				} catch (Exception e){
					log("Error while calling "+tag+", called by "+sender);
					log(e);
				}
			}
		}
	}

	/* Plugin loaders */

	public synchronized void registerPluginLoader(PluginLoader loader) {
		loaders.add(loader);
	}

	public synchronized void registerPluginLoader(PluginLoader loader, String[] extensions) {
		registerPluginLoader(loader);
		LoaderManager.INSTANCE.registerExtensions(extensions);
	}

	public synchronized void registerPluginLoader(PluginLoader loader, String extension) {
		registerPluginLoader(loader, new String[] {extension});
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
			return initializeInnerPlugin(packageName, (Plugin) plugin);
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
			log(e);
			return false;
		}
	}
	
	public synchronized boolean loadPluginByPath(Path path) throws Throwable {
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
		// 1. Tries to find an already loaded plugin with the same name.
		if (plugins.containsKey(name)) {
			return true;
		}

		// 2. If the plugin can be found in the plugins directory, it's loaded.
		Path path = getDefaultPluginDirPath(name);

		if (path.toFile().exists()) {
			for (PluginLoader loader : loaders) {
				if (loader.matchPath(path)) {
					loader.loadByPath(path);
					return true;
				}
			}
		}

		// 3. Tries to find an already loaded plugin with a similar name.
		if (plugins.values().stream().anyMatch(PluginProxy -> PluginProxy.isNameMatched(name))) {
			return true;
		}

		// 4. If any plugin can be found in the plugins directory without respect to their extensions, the first one will be loaded.
		File[] files = getPluginsDirPath().toFile().listFiles((file, s) -> FilenameUtils.removeExtension(s).equals(name));
		if (files != null) {
			if (files.length > 1) {
				log("Too many plugins with similar names (" + name + ")!");
			}
			return loadPluginByPath(files[0].toPath());
		}

		// 5. Otherwise, the plugin cannot be loaded by name.
		return false;
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
			savePluginsBlacklist();
		}
	}
	
	public void removePluginFromBlacklist(String name) {
		blacklistedPlugins.remove(name);
		savePluginsBlacklist();
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
		} catch (IOException e) { }
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
		if (corePath == null) {
			try {
				corePath = Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (URISyntaxException e) {
				corePath = Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().getFile());
			}
			debugBuild = Files.isDirectory(corePath);
		}
		return corePath;
	}

	public static Path getPluginsDirPath() {
		if(pluginsDirPath != null) {
			return pluginsDirPath;
		}
		Path path = getRootDirPath();
		if (debugBuild) {
			path = path.resolve("build").resolve("launch4j");
		}
		path = path.resolve("plugins");
		pluginsDirPath = path.normalize();
		return pluginsDirPath;
	}

	public static Path getDefaultPluginDirPath(String name) {
		Stack<File> paths = new Stack<>();
		paths.push(getPluginsDirPath().toFile());
		while (!paths.empty()) {
			File path = paths.pop();
			File[] files = path.listFiles();
			if(files==null || files.length==0) continue;
			for(File file : files){
				if(file.isFile()){
					if(FilenameUtils.getBaseName(file.toString()).equals(name)){
						return file.toPath();
					}
				} else if(file.isDirectory()){
					if(file.getName().equals(name)){
						return file.toPath();
					}
					paths.push(file);
				}
			}
		}
		return getPluginsDirPath();
	}

	public static Path getDataDirPath() {
		if(dataDirPath != null) {
			return dataDirPath;
		}
		Path path = getRootDirPath();
		if (debugBuild) {
			path = path.resolve("build");
		}
		path = path.resolve("data");
		if (!Files.isDirectory(path)) {
			path.toFile().mkdir();
			log("Created directory: " + path);
		}
		dataDirPath = path.normalize();
		return dataDirPath;
	}

	public static Path getAssetsDirPath() {
		if(assetsDirPath != null) {
			return assetsDirPath;
		}
		Path path = getRootDirPath();
		path = path.resolve("assets");
		if (!Files.isDirectory(path)) {
			path.toFile().mkdir();
			log("Created directory: " + path);
		}
		assetsDirPath = path.normalize();
		return assetsDirPath;
	}

	public static Path getRootDirPath() {
		if(rootDirPath != null) {
			return rootDirPath;
		}
		Path corePath = getCorePath();
		Path path;
		if (debugBuild) {
			path = corePath.resolve("../../../");
		} else {
			path = corePath.getParent().resolve("../");
		}
		rootDirPath = path.normalize();
		return rootDirPath;
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

	/* Manifest getter */

	public PluginConfig getPluginConfig(String name) {
		if (!plugins.containsKey(name)) {
			return null;
		}
		return plugins.get(name).getConfig();
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
