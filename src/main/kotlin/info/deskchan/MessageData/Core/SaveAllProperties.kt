package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Send signal to all plugins to save their properties
 * **/

@MessageData.Tag("core:save-all-properties")
class SaveAllProperties : MessageData