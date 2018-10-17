package info.deskchan.MessageData.CoreEvents

import info.deskchan.core.MessageData

/**
 * Program loading completed
 *
 * @property plugin Plugin name
 */
@MessageData.Tag("core-events:loading-complete")
class LoadingComplete : MessageData