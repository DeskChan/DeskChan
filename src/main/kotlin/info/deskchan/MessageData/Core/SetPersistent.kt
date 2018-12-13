package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData
import info.deskchan.core.Path
import java.net.URI

/**
 * Set message tag as persistent. It means that core will store last message sent to tag, so
 * every time any plugin registers message listener, it will receive last sent message immediately,
 * even if message was sent before message listener registration.
 *
 * @property tag Message tag
 */
@MessageData.Tag("core:set-persistent")
class SetPersistent(val tag: String) : MessageData