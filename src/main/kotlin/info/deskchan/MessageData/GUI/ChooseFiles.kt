package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Open file chooser dialog
 *
 * <b>Response type</b>: String or List<String>, as specified by @multiple parameter
 *
 * @property title Dialog title
 * @property filters Files filters to use in dialog
 * @property initialDirectory Directory to start
 * @property initialFilename Initial filename
 * @property multiple Select multiple files. False by default
 * @property saveDialog Is current dialog is "Save file" type or it "Open file" type
 * **/

@MessageData.Tag("gui:choose-file")
@MessageData.RequiresResponse
class ChooseFiles : MessageData {

    var title: String? = null
    var initialDirectory: String? = null
    var initialFilename: String? = null
    var multiple: Boolean? = null
    var saveDialog: Boolean? = null

    var filters: List<Filter>? = null

    companion object {
        val ResponseFormat: String? = null
    }

    class Filter(val description: String, val extensions: List<String>) : MessageData

}