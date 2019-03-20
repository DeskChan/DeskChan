package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set current sprite name to show on screen. Message will not be skipped.
 *
 * @property name Name of character image
 **/
@MessageData.Tag("gui:set-image")
class SetImage (val name: String) : MessageData