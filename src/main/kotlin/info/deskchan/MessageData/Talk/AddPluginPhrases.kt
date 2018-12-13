package info.deskchan.MessageData.Talk

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Load phrases pack. User cannot turn it off or control it other way
 *
 * @property value Phrases pack filename or list of filenames
 * **/

@MessageData.Tag("talk:add-plugin-phrases")
class AddPluginPhrases : MessageData{

    val value: Any

    constructor(value: String){
        this.value = value
    }

    constructor(value: Path){
        this.value = value.absolutePath
    }

    constructor(value: List<String>){
        this.value = value
    }
}