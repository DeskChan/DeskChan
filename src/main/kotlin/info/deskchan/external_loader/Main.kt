package info.deskchan.external_loader

import info.deskchan.core.*
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.Path


class Main : Plugin, PluginLoader {

    private lateinit var pluginProxy: PluginProxyInterface
    private val supportedPlugins = mutableMapOf<String, String>(
            ".py" to "Python"
    )

    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {
        this.pluginProxy = pluginProxy
        pluginProxy.setConfigField("name", pluginProxy.getString("external-loader-plugin-name"))
        val bit = if (CoreInfo.is64Bit()) "64" else "32"
        val sys: String
        if (SystemUtils.IS_OS_WINDOWS)
            sys = "win"
        else if (SystemUtils.IS_OS_MAC)
            sys = "osx"
        else if (SystemUtils.IS_OS_UNIX)
            sys = "unix"
        else
            sys = "unknown"

        supportedPlugins.put(sys + "-x" + bit + ".exe", "Executable")

        supportedPlugins.keys.forEach {
            PluginManager.getInstance().registerPluginLoader(this, it)
        }

        return true
    }

    override fun unload() = PluginManager.getInstance().unregisterPluginLoader(this)

    //override fun matchPath(file: File):Boolean {
    override fun matchPath(path: Path):Boolean {
        val file = path.toFile()
        val files = when {
            file.isDirectory -> file.listFiles().map{ it.name.toString() }
            file.isFile -> listOf(file.name)
            else -> emptyList<String>()
        }
        supportedPlugins.keys.forEach {
            val i = it
            files.forEach {
                if (it == "plugin" + i) return true
            }
        }
        return false
    }

    //override fun loadByPath(file: File) {
    override fun loadByPath(path: Path) {
        val file = path.toFile()
        when {
            file.isDirectory -> file.listFiles()
            else -> listOf(file).toTypedArray()
        }.forEach {
            if (it.name.startsWith("plugin"))
                loadPlugin(it)
        }
    }

    private fun loadPlugin(file: File) {
        println(file)
        var type: String? = null
        supportedPlugins.keys.forEach {
            if (file.name.endsWith(it)){
                type = it
                return@forEach
            }
        }
        if (type == null) return

        val config = PluginConfig(supportedPlugins[type.toString()].toString())
        val manifestPath = File(file.absolutePath + ".manifest")
        if (manifestPath.exists()) config.appendFromJson(file)

        val plugin = ExternalPlugin(file)

        PluginManager.getInstance().initializePlugin(file.parentFile.name, plugin, config)
    }

}
