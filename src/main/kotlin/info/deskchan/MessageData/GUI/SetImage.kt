package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set current sprite name to show on screen. Message will not be skipped.
 *
 * @property name Text of menu item
 **/
@MessageData.Tag("gui:set-image")
class SetImage (val name: String) : MessageData