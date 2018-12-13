package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Set user balloon from file
 *
 * @property name Balloon file
 **/
@MessageData.Tag("gui:set-user-balloon-path")
class SetUserBalloonPath (val name: Path) : MessageData