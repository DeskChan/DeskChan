package info.deskchan.MessageData.CoreEvents

import info.deskchan.core.MessageData

/**
 * Plugin was loaded
 *
 * @property plugin Plugin name
 */
@MessageData.Tag("core-events:plugin-load")
class PluginLoad(var plugin: String) : MessageData