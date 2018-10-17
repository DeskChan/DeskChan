package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Unregister event
 *
 * @property tag Event tag
 */
@MessageData.Tag("core:remove-event")
class RemoveEvent(val tag: String) : MessageData