package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Registering group of single actions that will be visible in click menu.
 *
 * @property name Text of menu item
 * @property actions Actions list
 **/
@MessageData.Tag("gui:register-simple-actions")
class RegisterSimpleActions  (
        val name: String,
        val actions: Collection<RegisterSimpleAction>
) : MessageData