package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Unregister command
 *
 * @property tag Command tag
 */
@MessageData.Tag("core:remove-command")
class RemoveCommand(val tag: String) : MessageData