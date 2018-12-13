package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Quit program
 *
 * @constructor Quit delay, in ms.
 * @property delay Quit delay, ms. 0 by default
 */
@MessageData.Tag("core:quit")
class Quit(val delay: Long = 0) : MessageData