package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Notification about character saying some text
 * This message called immediately after showing text on screen
 *
 * @property value Speech text
 * **/

@MessageData.Tag("DeskChan:just-said")
class JustSaid (val value: String) : MessageData