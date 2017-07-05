package info.deskchan.core

import java.io.IOException
import java.nio.file.Files

fun main(args: Array<String>) {
	val pluginManager = PluginManager.getInstance()
	pluginManager.initialize(args)
	pluginManager.tryLoadPluginByPackageName("info.deskchan.core_utils")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.groovy_support")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.gui_javafx")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.chat_window")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.talking_system")
	pluginManager.tryLoadPluginByPackageName("info.deskchan.telegram_api")
	try {
		val pluginsDirPath = PluginManager.getPluginsDirPath()
		val dirStream = Files.newDirectoryStream(pluginsDirPath)
		for (path in dirStream) {
			pluginManager.tryLoadPluginByPath(path)
		}
	} catch (e: IOException) {
		PluginManager.log(e)
	}
}
