package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Get character visibility.
 *
 * <b>Response type</b>: Boolean
 * **/

@MessageData.Tag("gui:is-character-visible")
@MessageData.RequiresResponse
class IsCharacterVisible : MessageData {
    companion object {
        val ResponseFormat: Boolean? = null
    }
}