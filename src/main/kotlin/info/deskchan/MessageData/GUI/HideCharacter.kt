package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Hide character from screen. This state will not be saved, so character will be shown on next program launch.
 **/
@MessageData.Tag("gui:hide-character")
class HideCharacter : MessageData