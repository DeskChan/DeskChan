package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Set current skin name of character. Message will not be skipped.
 *
 * @property name Text of menu item
 **/
@MessageData.Tag("gui:change-skin")
class ChangeSkin (val name: Path) : MessageData