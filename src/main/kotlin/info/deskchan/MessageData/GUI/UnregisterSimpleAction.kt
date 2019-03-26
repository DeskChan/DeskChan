package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Unregistering submenu with single actions from click menu
 *
 * @property name Text of menu item
 **/
@MessageData.Tag("gui:unregister-simple-action")
class UnegisterSimpleAction (
        val name: String
) : MessageData