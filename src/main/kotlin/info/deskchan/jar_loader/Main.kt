package info.deskchan.jar_loader

import info.deskchan.core.*
import java.io.File
import java.io.FilenameFilter
import java.net.URLClassLoader
import java.util.jar.Attributes
import java.util.jar.JarFile


class Main : Plugin, PluginLoader {

    private lateinit var pluginProxy: PluginProxyInterface

    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {
        this.pluginProxy = pluginProxy
        pluginProxy.setConfigField("name", pluginProxy.getString("jar-loader-plugin-name"))
        PluginManager.getInstance().registerPluginLoader(this, "jar")
        return true
    }

    override fun unload() = PluginManager.getInstance().unregisterPluginLoader(this)

    override fun matchPath(path: Path) = when {
        path.isDirectory() -> path.files (FilenameFilter { _, name -> name.endsWith(".jar") }).isNotEmpty()
        path.isFile -> path.endsWith(".jar")
        else -> false
    }

    override fun loadByPath(path: Path) {
        val jars = when {
            path.isDirectory() -> {
                val list = mutableListOf<File>()
                path.listFiles().forEach {
                    if (!it.isFile) return@forEach
                    var correctedPath = it as File
                    if (!it.toString().endsWith(".jar"))
                        correctedPath = path.resolveSibling(it.toString() + ".jar")
                    list.add(correctedPath)
                }
                list
            }
            else -> {
                var correctedPath = path as File
                if (!path.toString().endsWith(".jar"))
                    correctedPath = path.resolveSibling(path.toString() + ".jar")

                listOf(correctedPath)
            }
        }
        val urls = jars.map { it.toURI().toURL() }.toTypedArray()
        val loader = URLClassLoader(urls, javaClass.classLoader)
        jars.forEach { loadPlugin(it, loader) }
    }

    private fun loadPlugin(file: File, loader: ClassLoader) {
        val id = file.nameWithoutExtension

        val jar = JarFile(file)
        val manifestAttributes = jar.manifest.mainAttributes ?: Attributes()
        val mainClass = manifestAttributes.getValue("Main-Class")
        val className = mainClass ?: "Main"

        var cls: Class<*>
        try {
            cls = Class.forName(className, true, loader)
        } catch (e: Error) {
            try {
                cls = Class.forName(className, false, loader)
            } catch (e2: Error) {
                log("Unable to load plugin \"$id\"! Couldn't find class \"$className\".")
                return
            }
        }

        val plugin:Plugin?
        try {
            plugin = cls.newInstance() as? Plugin
            if (plugin == null) {
                log("The class \"$id\" is not an instance of Plugin!")
                return
            }
        } catch (e: Error){
            log(e)
            return
        }

        val config = PluginConfig()
        config.append("type", "Jar")
        manifestAttributes.forEach{
            config.append(it.key.toString(), it.value)
        }

        PluginManager.getInstance().initializePlugin(id, plugin, config)
    }

    fun Attributes.groupValues(attribute: String): Set<String> {
        val value = this.getValue(attribute)
        if (value != null) {
            return setOf(value)
        }

        return (1..1000)
                .map { this.getValue("$attribute-$it") }
                .takeWhile { it != null }
                .toSet()
    }

    /*fun Attributes.groupValueByKey(attribute: String): Map<String, String> {
        val value = this.getValue(attribute)
        if (value != null) {
            return mapOf(DEFAULT_LANGUAGE_KEY to value)
        }

        val regexp = "$attribute-(\\S+)".toRegex()
        return this.entries
                .map {
                    val matches = regexp.matchEntire(it.key.toString())
                    val key = matches?.groups?.get(1)?.value
                    if (key != null) {
                        Pair(key, it.value.toString())
                    } else {
                        null
                    }
                }
                .filterNotNull()
                .toMap()
    }*/

    fun log(obj: Any) = when (obj) {
        is Throwable -> pluginProxy.log(obj)
        else -> pluginProxy.log(obj.toString())
    }

}
