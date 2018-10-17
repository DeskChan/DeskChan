package info.deskchan.MessageData.CoreEvents

import info.deskchan.core.MessageData

/**
 * Plugin was unloaded
 *
 * @property plugin Plugin name
 */
@MessageData.Tag("core-events:plugin-unload")
class PluginUnload(var plugin: String) : MessageData