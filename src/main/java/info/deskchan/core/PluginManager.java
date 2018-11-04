package info.deskchan.core;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

public class PluginManager {

	private static final PluginManager instance = new PluginManager();

	private final Map<String, PluginProxy> plugins = new HashMap<>();

	// This map contain keys of two types:
	//  - some:tag  - just common messages
	//  - some:tag# - special messages: answers, passing inside alternatives chains and so on
	// This separation was added in order to prevent getting common and special messages duplicating each other
	// when you're subscribing to any message
	// To remove such separation, you need to change this files inside core: PluginManager, CorePlugin, PluginProxy
	private final Map<String, Set<MessageListener>> messageListeners = new HashMap<>();
	private final List<PluginLoader> loaders = new ArrayList<>();
	private final Set<String> blacklistedPlugins = new HashSet<>();
	private String[] args;
	private static OutputStream logStream = null;

	private final Map<String, Object> persistentMessages = new HashMap<>();

	private static boolean debugBuild = false;
    private static String[] debugBuildFolders = { "build", "out" };

	Set<MessageListener> getMessageListeners(String key){
		int delimiterPas = key.indexOf('#');
		if (delimiterPas >= 0)
			key = key.substring(0, delimiterPas + 1);

		return messageListeners.get(key);
	}

	/* Paths cache */

	private static Path corePath = null;
	private static Path pluginsDirPath = null;
	private static Path dataDirPath = null;
	private static Path rootDirPath = null;
	private static Path assetsDirPath = null;

	/* Singleton */
	
	private PluginManager() { }
	
	public static PluginManager getInstance() {
		return instance;
	}
	
	/* Core initialization */
	
	void initialize(String[] args) {
		this.args = args;
		Path logFile = getDataDirPath().resolve("DeskChan.log");
		try {
			logFile.createNewFile();
			logStream = new FileOutputStream(logFile);
		} catch (IOException e) {
			log(e);
		}
		CoreInfo.printInfo();
		tryLoadPluginByClass(CorePlugin.class);
		loadPluginsBlacklist();
		getCorePath();

		try {
			PluginProxy.Companion.updateResourceBundle();
		} catch (Exception e){
			log(e);
		}
	}
	
	public String[] getArgs() {
		return args;
	}

	public static boolean isDebugBuild() {
		return debugBuild;
	}

	Set<String> getNamesOfLoadedPlugins() {
		synchronized (plugins) {
			return plugins.keySet();
		}
	}

	public PluginProxyInterface getPlugin(String name){
		synchronized (plugins) {
			return plugins.get(name);
		}
	}

	/* Plugin initialization and unloading */

	/** Plugin initialization. Use other methods of this class if you haven't all needed components
	 * You will not receive any information about if plugin has been loaded successfully.
	 *
	 * @param id Plugin name
	 * @param plugin Plugin object
	 * @param config Config (you can parse it from file, create by yourself or pass null)
	 * @return Is initialization has started successfully.
	 *
	 * @throws Throwable Plugin with such name already exist
	 */
	public boolean initializePlugin(String id, Plugin plugin, PluginConfig config) throws Throwable {
		synchronized (plugins){

		if (plugins.containsKey(id)) {
			throw new Throwable("Cannot load plugin \"" + id + "\": plugin with such name already exist");
		}

		if (blacklistedPlugins.contains(id)) {
			return false;
		}
		if (!plugins.containsKey(id)) {
			new Debug.TimeTest(){
				@Override
				void run(){
					try {
						PluginProxy entity = PluginProxy.Companion.create(plugin, id, config);
						if (entity != null) {
							plugins.put(id, entity);
							if (config != null) {
								LoaderManager.INSTANCE.registerExtensions(config.getExtensions());
							}
							log("Registered plugin: " + id);
							sendMessage("core", "core-events:plugin-load", id);
						}
					} catch (Exception e) {
						log("Plugin not registered: " + id);
						log(e);
					}
				}
			};
			return true;
		}
		return false;
	}}

	/** Plugin initialization. Plugin will be marked as inner plugin with default configuration. <br>
	 * Other functional equals {@link #initializePlugin(String, Plugin, PluginConfig) this method}
	 */
	public boolean initializeInnerPlugin(String id, Plugin plugin) throws Throwable {
		return initializePlugin(id, plugin, PluginConfig.Companion.getInternal().clone());
	}

	/**  Plugin will be unregistered from memory. **/
	void unregisterPlugin(PluginProxyInterface pluginProxy) {
		unregisterPlugin(pluginProxy.getId());
	}

	/**  Plugin will be unregistered from memory. **/
	void unregisterPlugin(String id) {
		synchronized (plugins) {
			plugins.remove(id);
		}
		log("Unregistered plugin: " + id);
		sendMessage("core", "core-events:plugin-unload", id);
	}

	/**  Plugin will be unloaded from memory.
	 * @return Is plugin has existed and has been unloaded successfully
	 */
	public boolean unloadPlugin(String name) {
		synchronized (plugins) {
			PluginProxy plugin = plugins.get(name);
			if (plugin != null) {
				plugin.unload();
				return true;
			}
			return false;
		}
	}
	
	/* Message bus */

	/**  Register tag listener. All messages that sending to <b>tag</b> will be automatically sent to <b>listener.handle</b>.  **/
	void registerMessageListener(String tag, MessageListener listener) {
		Set<MessageListener> listeners = messageListeners.get(tag);
		if (listeners == null) {
			listeners = new HashSet<>();
			messageListeners.put(tag, listeners);
		}
		if (tag.equals("core-events:plugin-load")) {
			synchronized (plugins) {
				for (String id : plugins.keySet()) {
					listener.handleMessage("core", "core-events:plugin-load", id);
				}
			}
		}
		listeners.add(listener);
	}

	/**  Unregister tag listener. **/
	void unregisterMessageListener(String tag, MessageListener listener) {
		Set<MessageListener> listeners = messageListeners.get(tag);
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.size() == 0) {
				messageListeners.remove(tag);
			}
		}
	}

	void setPersistentMessage(String message){
		persistentMessages.putIfAbsent(message, null);
	}

	/**  Get listeners count. **/
	int getMessageListenersCount(String tag) {
		Set<MessageListener> listeners = messageListeners.get(tag);
		if (listeners != null) {
			return listeners.size();
		}
		return 0;
	}

	/**
	 * Send message to tag. <br>
	 * You cannot call this method directly to not falsify messages from other plugins
	 * @param sender Name of plugin that sends message
	 * @param tag Tag of message
	 * @param data Additional data that will be sent with query, can be null
	 */
	void sendMessage(String sender, String tag, Object data) {
		Object serializedData = MessageDataUtils.serialize(data);

		if (persistentMessages.containsKey(tag)){
			persistentMessages.put(tag, serializedData);
		}

		Set<MessageListener> listeners = getMessageListeners(tag);
		if (listeners == null || listeners.size() == 0)
			return;

		synchronized (messageListeners) {
			for (MessageListener listener : listeners) {
				Debug.TimeTest send = new Debug.TimeTest() {
					@Override
					void run() {
						try {
							listener.handleMessage(sender, tag, serializedData);
						} catch (Throwable e) {
							if (!tag.equals("core-events:error"))
								log(sender, new Exception("Error while calling " + tag + ", called by " + sender, e));
						}
					}
				};
			}
		}
	}

	/* Plugin loaders */
	/** Register loader object that can load other plugins.
	 * @param loader Plugin loader
	*/
	public synchronized void registerPluginLoader(PluginLoader loader) {
		loaders.add(loader);
	}

	/** Register loader object that can load other plugins.
	 * @param loader Plugin loader
	 * @param extensions Array of file extensions that new plugin loader can work with
	 */
	public synchronized void registerPluginLoader(PluginLoader loader, String[] extensions) {
		registerPluginLoader(loader);
		LoaderManager.INSTANCE.registerExtensions(extensions);
	}

	/** Register loader object that can load other plugins.
	 * @param loader Plugin loader
	 * @param extension File extensions that new plugin loader can work with
	 */
	public synchronized void registerPluginLoader(PluginLoader loader, String extension) {
		registerPluginLoader(loader, new String[] {extension});
	}

	/** Unregister loader from program. All plugins that loaded with this loader will stay loaded. **/
	public synchronized void unregisterPluginLoader(PluginLoader loader) {
		loaders.remove(loader);
	}

	/** Loading plugin by its class type.
	 * @param cls Class type
	 * @see #initializePlugin(String, Plugin, PluginConfig)
	 */
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

	/** Try to load plugin by its class type.
	 * @param cls Class type
	 * @see #loadPluginByClass(Class)
	 */
	public boolean tryLoadPluginByClass(Class cls) {
		try {
			return loadPluginByClass(cls);
		} catch (Throwable e) {
			return false;
		}
	}

    private static boolean IsPathEndsWithList(Path path, String[] names){
	    for(String name: names){
	        if (path.endsWith(name))
	            return true;
        }
		return false;
    }

	/** Load plugin by its class name.
	 * @param className Class name
	 * @see #loadPluginByClass(Class)
	 */
	public boolean loadPluginByClassName(String className) throws Throwable {
		Class cls = getClass().getClassLoader().loadClass(className);
		return loadPluginByClass(cls);
	}

	/** Try to load plugin by its class name.
	 * @param className Class name
	 * @see #loadPluginByClass(Class)
	 */
	public boolean tryLoadPluginByClassName(String className) {
		try {
			return loadPluginByClassName(className);
		} catch (Throwable e) {
			return false;
		}
	}

	/** Load plugin by its package name. Program searches package and gets Main class from it.
	 * @param packageName Package name
	 * @see #loadPluginByClass(Class)
	 */
	public boolean loadPluginByPackageName(String packageName) throws Throwable {
		return loadPluginByClassName(packageName + ".Main");
	}

	/** Try to load plugin by its package name.
	 * @param packageName Package name
	 * @see #loadPluginByPackageName(String)
	 */
	public boolean tryLoadPluginByPackageName(String packageName) {
		try {
			return loadPluginByPackageName(packageName);
		} catch (Throwable e) {
			log(e);
			return false;
		}
	}

	/** Load plugin by path. Program searches appropriate loader for file and gives it to loader.
	 * @param path Path to plugin file
	 * @throws Throwable If could not match loader for plugin
	 */
	public synchronized boolean loadPluginByPath(Path path) throws Throwable {
		for (PluginLoader loader : loaders) {
			if (loader.matchPath(path)) {
				loader.loadByPath(path);
				return true;
			}
		}
		throw new Exception("Could not match loader for plugin " + path.toString());
	}

	/** Try to load plugin by path. Program searches appropriate loader for file and gives it to loader.
	 * @param path Path to plugin file
	 * @see #tryLoadPluginByPath(Path)
	 */
	public boolean tryLoadPluginByPath(Path path) {
		try {
			return loadPluginByPath(path);
		} catch (Throwable e) {
			log(e.getMessage());
		}
		return false;
	}

	/** Load plugin by name. Program searches 'plugins' subfolder for plugin file.
	 * @param name Plugin name
	 * @throws Throwable If plugin was found but cannot be loaded
	 */
	public boolean loadPluginByName(String name) throws Throwable {
		synchronized (plugins){

		// 1. Tries to find an already loaded plugin with the same name.
		if (plugins.containsKey(name)) {
			return true;
		}

		try {
			loadPluginByPackageName("info.deskchan." + name);
			return true;
		} catch (Throwable e) { }


		// 2. If the plugin can be found in the plugins directory, it's loaded.
		Path path = new Path(getDefaultPluginDirPath(name));

		if (path.exists()) {
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
		Set<Path> files = getPluginsDirPath().files((file, s) -> FilenameUtils.removeExtension(s).equals(name));
		if (files.size() > 1) {
			log("Too many plugins with similar names (" + name + ")!");
		}
		try {
			return loadPluginByPath(files.iterator().next());
		} catch (Exception e){
			log(e.getMessage());
		}

		// 5. Otherwise, the plugin cannot be loaded by name.
		return false;
	}}

	/** Load plugin by name.
	 * @param name Plugin name
	 * @see #loadPluginByName(String)
	 */
	public boolean tryLoadPluginByName(String name) {
		try {
			return loadPluginByName(name);
		} catch (Throwable e) {
			log(e);
		}
		return false;
	}

	/* Application finalization */

	/** Unload all plugins and close immediately. **/
	void quit() {
		synchronized (plugins){

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
	}}

	/** Get list of plugins. **/
	public List<String> getPlugins() {
		synchronized (plugins) {
			return new ArrayList<>(plugins.keySet());
		}
	}

	/* Plugins blacklist */

	/** Get list of plugins in blacklist. **/
	public List<String> getBlacklistedPlugins() {
		return new ArrayList<>(blacklistedPlugins);
	}

	/** Add plugin to blacklist. It will be unloaded immediately. **/
	public void addPluginToBlacklist(String name) {
		if (!name.equals("core")) {
			blacklistedPlugins.add(name);
			unloadPlugin(name);
			savePluginsBlacklist();
		}
	}

	/** Remove plugin from blacklist. You need to load it manually if you want it to be loaded. **/
	public void removePluginFromBlacklist(String name) {
		Debug.printTraceBack();
		blacklistedPlugins.remove(name);
		savePluginsBlacklist();
	}

	/** Load blacklist from file. **/
	private void loadPluginsBlacklist() {
		Path blacklist = getPluginDataDirPath("core").resolve("blacklisted-plugins.txt");
		if (!blacklist.exists()) return;

		try {
			for (String line : blacklist.readAllLines()) {
				if (line.trim().length() > 0)
					blacklistedPlugins.add(line);
			}
		} catch (Exception e){
			log(e);
		}
	}

	/** Save blacklist to file. **/
	private void savePluginsBlacklist() {
		try {
			BufferedWriter writer = getPluginDataDirPath("core").resolve("blacklisted-plugins.txt").newBufferedWriter();
			for (String id : blacklistedPlugins) {
				writer.write(id);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			log(e);
		}
	}

	public void saveProperties(){
		synchronized (plugins) {
			for (Map.Entry<String, PluginProxy> plugin : plugins.entrySet())
				sendMessage("core", plugin.getKey() + ":save-properties", null);
		}
	}
	
	/* Plugins and data directories */

	/** Get 'bin' folder path. **/
	public static Path getCorePath() {
		if (corePath == null) {
			try {
				corePath = new Path(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (URISyntaxException e) {
				corePath = new Path(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().getFile());
			}
			debugBuild = corePath.isDirectory();
		}
		return corePath;
	}

	/** Get 'plugins' folder path. **/
	public static Path getPluginsDirPath() {
		if(pluginsDirPath != null) {
			return pluginsDirPath;
		}
		Path path = getRootDirPath();
		if (debugBuild) {
			path = path.resolve("build").resolve("launch4j");
		}
		path = path.resolve("plugins");
		pluginsDirPath = path;
		createPath(pluginsDirPath);
		return pluginsDirPath;
	}

	/** Get 'plugins/%pluginName%' folder path. **/
	public static Path getDefaultPluginDirPath(String name) {
		Stack<File> paths = new Stack<>();
		paths.push(getPluginsDirPath());
		while (!paths.empty()) {
			File path = paths.pop();
			File[] files = path.listFiles();
			if(files == null || files.length == 0) continue;
			for(File file : files){
				if(file.isFile()){
					if(FilenameUtils.getBaseName(file.toString()).equals(name)){
						return new Path(file);
					}
				} else if(file.isDirectory()){
					if(file.getName().equals(name)){
						return new Path(file);
					}
					paths.push(file);
				}
			}
		}
		return getPluginsDirPath();
	}

	/** Get 'data' folder path. **/
	public static Path getDataDirPath() {
		if(dataDirPath != null) {
			return dataDirPath;
		}
		Path path = getRootDirPath();
		if (debugBuild) {
			path = path.resolve("build");
		}
		path = path.resolve("data");
		if (!path.isDirectory()) {
			path.mkdir();
			log("Created directory: " + path);
		}
		dataDirPath = path;
		createPath(dataDirPath);
		return dataDirPath;
	}

	/** Get 'assets' folder path. **/
	public static Path getAssetsDirPath() {
		if(assetsDirPath != null) {
			return assetsDirPath;
		}
		Path path = getRootDirPath();
		path = path.resolve("assets");
		if (!path.isDirectory()) {
			path.mkdir();
			log("Created directory: " + path);
		}
		assetsDirPath = path;
		createPath(assetsDirPath);
		return assetsDirPath;
	}

	/** Get DeskChan folder path. **/
	public static Path getRootDirPath() {
		if(rootDirPath != null) {
			return rootDirPath;
		}
		Path corePath = getCorePath();
		Path path;
		if (debugBuild) {
			path = corePath;
			try {
                while (!IsPathEndsWithList(path,debugBuildFolders))
					path = path.getParentPath();
				path = path.getParentPath();
			} catch (Exception e){
				log("Error while locating root path with path: "+corePath);
			}
		} else {
			path = corePath.getParentPath().resolve("../");
		}
		rootDirPath = path;
		return rootDirPath;
	}

	/** Get 'data/%pluginName%' folder path. **/
	public static Path getPluginDataDirPath(String id) {
		final Path baseDir = getDataDirPath();
		final Path dataDir = baseDir.resolve(id);
		if (!dataDir.isDirectory()) {
			dataDir.mkdirs();
			log("Created directory: " + dataDir.toString());
		}
		return dataDir;
	}

	/** Get plugin config by plugin name. **/
	public PluginConfig getPluginConfig(String name) {
		synchronized (plugins) {
			if (!plugins.containsKey(name)) {
				return null;
			}
			return plugins.get(name).getConfig();
		}
	}

	private static void createPath(Path path){
		if (!path.exists()) {
			try {
				path.mkdir();
			} catch (Exception e) {
				log(e);
			}
		}
	}

	/* Logging */

	/** Log info to file and console.
	 * You cannot call this method, use your plugin's proxy. **/

	static void log(String id, String message){
		log(id,message,LoggerLevel.INFO);
	}

	static void log(String id, String message,LoggerLevel level) {
		String text = id + ": " + message;

		if (level.equals(LoggerLevel.ERROR)){
			System.err.println(text);
			writeStringToLogStream(text);
		} else if (level.getValue() >= LoggerLevel.WARN.getValue() && level.getValue() <= LoggerLevel.TRACE.getValue()){
			System.out.println(text);
			writeStringToLogStream(text);
		}
	}

	static void writeStringToLogStream(String text){
		if (logStream != null) {
			try {
				logStream.write((text + "\n").getBytes("UTF-8"));
			} catch (IOException e) {
				logStream = null;
				log(e);
			}
		}
	}


	static void log(String id, String message, List<Object> stacktrace) {
		log(id, message,LoggerLevel.ERROR);
		for (Object line : stacktrace) {
			log(id, "   at " + line.toString(),LoggerLevel.ERROR);
		}
	}

	static void log(Throwable e) {
		log("core", e);
	}

	static void log(String message) {
		log("core", message);
	}

	static void log(String id, Throwable e) {
		while (e.getCause() != null) e = e.getCause();

		List<Object> stackTrace = new ArrayList<>(Arrays.asList(e.getStackTrace()));
		log(id, e.getClass().getSimpleName() + ":  " + e.getMessage(), stackTrace);

		Map error = new HashMap();
		error.put("class", e.getClass().getSimpleName());
		error.put("message", e.getMessage());
		error.put("stacktrace", stackTrace);

		getInstance().sendMessage(id, "core-events:error", error);
	}

}
