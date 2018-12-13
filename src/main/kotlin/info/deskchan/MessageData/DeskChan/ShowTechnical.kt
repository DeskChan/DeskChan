package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Show some text in special technical window.
 *
 * @property name Window name
 * @property text Window text
 * **/

@MessageData.Tag("DeskChan:show-technical")
class ShowTechnical(var text: String) : MessageData {

    var name: String? = null

    constructor(text: String, name: String) : this(text){
        this.name = name
    }

}