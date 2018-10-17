package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Get all links that subscribed to event.
 *
 * <b>Response type</b>: List<Map<String, Object>>
 *
 * List of links, where every element contains command <b>tag</b>,
 * <b>rule</b> if link have rule and <b>message</b> if link contains message.
 *
 * @property event Event tag
 *
 * **/

@MessageData.Tag("core:get-commands-match")
@MessageData.RequiresResponse
class GetCommandsMatch(val event: String) : MessageData {
    companion object {
        val ResponseFormat: List<Map<String, Any>>? = null
    }
}