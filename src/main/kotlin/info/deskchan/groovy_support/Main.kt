package info.deskchan.groovy_support

import groovy.lang.GroovyShell
import info.deskchan.core.*
import org.codehaus.groovy.control.CompilerConfiguration

class Main : Plugin, PluginLoader {

    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {
        PluginManager.getInstance().registerPluginLoader(this)
        try {
            PluginManager.getInstance().initializePlugin("scenario", ScenarioPlugin(), PluginConfig.Internal)
        } catch (e: Throwable) {
            pluginProxy.log(e)
        }
        pluginProxy.setResourceBundle("info/deskchan/groovy_support/strings")
        pluginProxy.setConfigField("name", pluginProxy.getString("loader-plugin-name"))

        return true
    }

    override fun unload() {
        PluginManager.getInstance().unregisterPluginLoader(this)
    }

    override fun matchPath(path: Path): Boolean {
        return if (path.isDirectory()) {
            path.resolve("plugin.groovy").canRead()
        } else {
            path.name.toString().endsWith(".groovy")
        }
    }

    @Throws(Throwable::class)
    override fun loadByPath(path: Path) {
        var path = path
        var id: String
        if (path.isDirectory()) {
            id = path.name
            path = path.resolve("plugin.groovy")
        } else {
            if (path.name == "plugin.groovy")
                id = path.getParentPath()!!.name.toString()
            else {
                id = path.name.toString()
                id = id.substring(0, id.length - 7)
            }
        }
        System.setProperty("groovy.grape.report.downloads", "true")
        val compilerConfiguration = CompilerConfiguration()
        compilerConfiguration.sourceEncoding = "UTF-8"
        compilerConfiguration.scriptBaseClass = "info.deskchan.groovy_support.GroovyPlugin"
        compilerConfiguration.setClasspath(path.parent.toString())
        val groovyShell = GroovyShell(compilerConfiguration)
        val script = groovyShell.parse(path)
        val plugin = script as GroovyPlugin
        plugin.pluginDirPath = path.getParentPath()
        val config = PluginConfig("Groovy")
        path = path.getParentPath()!!.resolve("manifest.json")
        config.appendFromJson(path)
        PluginManager.getInstance().initializePlugin(id, plugin, config)
    }
}
