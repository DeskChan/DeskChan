@file:JvmName("PluginUtils")

package info.deskchan.core


fun createPluginConfig(): PluginConfigInterface = PluginConfig()

fun createPluginConfig(type: String): PluginConfigInterface = PluginConfig(type)
