package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Registering submenu with single actions that will be visible in click menu
 * Recommended not to use this message but register event link to gui:menu-action
 *
 * @property name Text of menu item
 * @property msgTag Message tag associated with menu item
 * @property msgData Message data that will be sent to msgTag
 **/
@MessageData.Tag("gui:register-simple-action")
class RegisterSimpleAction (
        val name: String,
        val msgTag: String,
        val msgData: Any?
) : MessageData