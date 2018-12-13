package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Set current interface style from file.
 *
 * @property name Interface style file
 **/
@MessageData.Tag("gui:set-interface-style")
class SetInterfaceStyle (val name: Path) : MessageData