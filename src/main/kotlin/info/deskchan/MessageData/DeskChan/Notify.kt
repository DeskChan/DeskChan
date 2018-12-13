package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Any notification received to show in notification board.
 * If no property is specified, NOTIFY phrase will only be thrown
 *
 * @property message Message text to show by technical window.
 * @property speech Text to say by character, which notification will be accompanied with. NOTIFY by default.
 * @property speechIntent Intent for phrase to say by character, which notification will be accompanied with.
 * **/

@MessageData.Tag("DeskChan:notify")
class Notify : MessageData {

    @MessageData.FieldName("speech-intent")
    var speechIntent: String? = "NOTIFY"

    var message: String? = null
    var speech: String? = null

}