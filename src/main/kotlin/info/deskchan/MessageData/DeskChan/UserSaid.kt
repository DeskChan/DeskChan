package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Notification about user speech received by any source
 *
 * @property value Speech text
 * **/

@MessageData.Tag("DeskChan:user-said")
class UserSaid (val value: String) : MessageData