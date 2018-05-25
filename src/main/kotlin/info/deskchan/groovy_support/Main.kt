package info.deskchan.groovy_support

import groovy.lang.GroovyShell
import info.deskchan.core.*
import org.codehaus.groovy.control.CompilerConfiguration

import java.nio.file.Files
import java.nio.file.Path

class Main : Plugin, PluginLoader {

    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {
        PluginManager.getInstance().registerPluginLoader(this)
        try {
            PluginManager.getInstance().initializePlugin("scenario", ScenarioPlugin(), PluginConfig.Internal)
        } catch (e: Throwable) {
            pluginProxy.log(e)
        }
        pluginProxy.setResourceBundle("info/deskchan/groovy_support/strings")

        return true
    }

    override fun unload() {
        PluginManager.getInstance().unregisterPluginLoader(this)
    }

    override fun matchPath(path: Path): Boolean {
        return if (Files.isDirectory(path)) {
            Files.isReadable(path.resolve("plugin.groovy"))
        } else {
            path.fileName.toString().endsWith(".groovy")
        }
    }

    @Throws(Throwable::class)
    override fun loadByPath(path: Path) {
        var path = path
        var id: String
        if (Files.isDirectory(path)) {
            id = path.fileName.toString()
            path = path.resolve("plugin.groovy")
        } else {
            if (path.fileName.toString() == "plugin.groovy")
                id = path.parent.fileName.toString()
            else {
                id = path.fileName.toString()
                id = id.substring(0, id.length - 7)
            }
        }
        System.setProperty("groovy.grape.report.downloads", "true")
        val compilerConfiguration = CompilerConfiguration()
        compilerConfiguration.sourceEncoding = "UTF-8"
        compilerConfiguration.scriptBaseClass = "info.deskchan.groovy_support.GroovyPlugin"
        compilerConfiguration.setClasspath(path.parent.toString())
        val groovyShell = GroovyShell(compilerConfiguration)
        val script = groovyShell.parse(path.toFile())
        val plugin = script as GroovyPlugin
        plugin.pluginDirPath = path.parent
        val config = PluginConfig("Groovy")
        path = path.parent.resolve("manifest.json")
        config.appendFromJson(path.toFile())
        PluginManager.getInstance().initializePlugin(id, plugin, config)
    }
}
