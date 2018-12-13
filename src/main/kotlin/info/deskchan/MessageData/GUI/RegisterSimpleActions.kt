package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Registering single action that will be visible in click menu.
 * Recommended not to use this message but register event link to gui:menu-action
 *
 * @property name Text of menu item
 * @property msgTag Tag to call when menu item will be chosen
 * @property msgData Data to send with tag when menu item will be chosen
 **/
@MessageData.Tag("gui:register-simple-actions")
class RegisterSimpleActions  (
        val name: String,
        val actions: Collection<RegisterSimpleAction>
) : MessageData