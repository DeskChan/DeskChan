package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Registering submenu with single actions that will be visible in click menu
 *
 * @property name Text of menu item
 * @property actions List of actions
 **/
@MessageData.Tag("gui:register-simple-action")
class RegisterSimpleAction (
        val name: String,
        val msgTag: String,
        val msgData: Any?
) : MessageData