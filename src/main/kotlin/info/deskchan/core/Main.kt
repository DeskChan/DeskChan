package info.deskchan.core

import java.io.IOException

fun main(args: Array<String>) {
	val pluginManager = PluginManager.getInstance()
	pluginManager.initialize(args)
	pluginManager.tryLoadPluginByPackageName("info.deskchan.core_utils")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.jar_loader")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.groovy_support")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.gui_javafx")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.talking_system")
	try {
		LoaderManager.loadPlugins()
	} catch (e: IOException) {
		PluginManager.log(e)
	}
	pluginManager.sendMessage("core","core-events:loading-complete",null)
}
