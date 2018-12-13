package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Request user speech.
 * Next speech that will be sent to DeskChan:user-said,
 * will be grabbed and resent as response, instead of sending through speech handlers.
 *
 * <b>Response type</b>: String
 *
 * @property expected List of lines of what expected from user to say
 * **/

@MessageData.Tag("DeskChan:request-user-speech")
@MessageData.RequiresResponse
class RequestUserSpeech(var expected: List<String>) : MessageData {
    companion object {
        val ResponseFormat: String? = null
    }
}