package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Unregister event-command link
 *
 * @property eventName Event tag
 * @property commandName Command tag
 */
@MessageData.Tag("core:remove-event-link")
class RemoveEventLink(val eventName: String, val commandName: String) : MessageData