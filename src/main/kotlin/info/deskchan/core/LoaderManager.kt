package info.deskchan.core

import java.io.FilenameFilter

typealias PluginFiles = Set<Path>


object LoaderManager {

    /** Extensions that registered loaders can interpret as plugin. **/
    private val registeredExtensions = mutableSetOf<String>()
    /** 'plugins' path. **/
    private val pluginsDirPath = PluginManager.getPluginsDirPath()

    /** Scan plugins directory to runnable plugins. **/
    private fun scanPluginsDir(): PluginFiles {
        val loadedPlugins = PluginManager.getInstance().namesOfLoadedPlugins
        return pluginsDirPath.files(FilenameFilter { _, name -> !loadedPlugins.contains(name) })
    }

    /** Automatically load all plugins from 'plugin' directory. **/
    internal fun loadPlugins() {
        var unloadedPlugins = scanPluginsDir()

        var loaderCount: Int
        do {
            loaderCount = registeredExtensions.size
            unloadedPlugins = unloadedPlugins
                    .loadFilePlugins()
                    .loadDirectoryPlugins()
        } while (unloadedPlugins.isNotEmpty() && loaderCount != registeredExtensions.size )
        
        unloadedPlugins
                .loadRestPlugins()
                .forEach { PluginManager.log("Could not match loader for plugin ${it.name}") }
    }

    /** Iterates over all registered extensions to find loadable plugins. **/
    private fun PluginFiles.loadFilePlugins(): PluginFiles {
        val unloadedPlugins = this.toMutableSet()
        val extensions = registeredExtensions
        extensions.forEach { ext ->
            this.filter { it.extension == ext }.forEach { unloadedPlugins.tryLoadPlugin(it) }
        }
        return unloadedPlugins
    }

    /** Iterates over all first level directories to find loadable plugins. **/
    private fun PluginFiles.loadDirectoryPlugins(): PluginFiles {
        val unloadedPlugins = this.toMutableSet()
        val (loaders, plugins) = this
                .filter { it.isDirectory }
                .partition { it.name.endsWith("support") || it.name.endsWith("loader") }
        (loaders + plugins).forEach { unloadedPlugins.tryLoadPlugin(it) }
        return unloadedPlugins
    }

    /** Tries to load all plugins that wasn't loaded previously. **/
    private fun PluginFiles.loadRestPlugins(): PluginFiles {
        val unloadedPlugins = this.toMutableSet()
        this.forEach { unloadedPlugins.tryLoadPlugin(it) }
        return unloadedPlugins
    }

    /** Tries to load plugin that wasn't loaded previously. **/
    private fun MutableSet<Path>.tryLoadPlugin(file: Path) {
        if (PluginManager.getInstance().tryLoadPluginByPath(file)) {
            this.remove(file)
        }
    }

    fun registerExtensions(vararg extensions: String) = registeredExtensions.addAll(extensions)
    fun registerExtensions(extensions: List<String>) = registerExtensions(*extensions.toTypedArray())

}
