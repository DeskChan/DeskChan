package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Set character balloon from file
 *
 * @property name Balloon file
 **/
@MessageData.Tag("gui:set-character-balloon-path")
class SetCharacterBalloonPath (val name: Path) : MessageData