package info.deskchan.core

import java.io.File

typealias PluginFiles = Set<File>


object LoaderManager {

    private val registeredExtensions = mutableSetOf<String>()
    private val pluginsDirPath = PluginManager.getPluginsDirPath()

    private fun scanPluginsDir(): PluginFiles {
        val loadedPlugins = PluginManager.getInstance().namesOfLoadedPlugins
        return pluginsDirPath.toFile().listFiles({ _, name -> !loadedPlugins.contains(name) }).toSet()
    }

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

    private fun PluginFiles.loadFilePlugins(): PluginFiles {
        val unloadedPlugins = this.toMutableSet()
        val extensions = registeredExtensions.toSet()
        extensions.forEach { ext ->
            this.filter { it.extension == ext }.forEach { unloadedPlugins.tryLoadPlugin(it) }
        }
        return unloadedPlugins
    }

    private fun PluginFiles.loadDirectoryPlugins(): PluginFiles {
        val unloadedPlugins = this.toMutableSet()
        val (loaders, plugins) = this
                .filter { it.isDirectory }
                .partition { it.name.endsWith("support") || it.name.endsWith("loader") }
        (loaders + plugins).forEach { unloadedPlugins.tryLoadPlugin(it) }
        return unloadedPlugins
    }

    private fun PluginFiles.loadRestPlugins(): PluginFiles {
        val unloadedPlugins = this.toMutableSet()
        this.forEach { unloadedPlugins.tryLoadPlugin(it) }
        return unloadedPlugins
    }

    private fun MutableSet<File>.tryLoadPlugin(file: File) {
        if (PluginManager.getInstance().tryLoadPluginByPath(file.toPath())) {
            this.remove(file)
        }
    }

    fun registerExtensions(vararg extensions: String) = registeredExtensions.addAll(extensions)
    fun registerExtensions(extensions: List<String>) = registerExtensions(*extensions.toTypedArray())

}
