package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Open folder chooser dialog
 *
 * <b>Response type</b>: String
 *
 * @property title Dialog title
 * @property initialDirectory Directory to start
 * **/

@MessageData.Tag("gui:choose-directory")
@MessageData.RequiresResponse
class ChooseDirectory : MessageData {

    var title: String? = null
    var initialDirectory: String? = null

    companion object {
        val ResponseFormat: String? = null
    }

}