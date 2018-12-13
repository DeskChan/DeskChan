package info.deskchan.MessageData.Speech

import info.deskchan.core.MessageData

/**
 * Extract data of specific type from speech
 *
 * @property speech Speech to extract data from
 * @property type Type of data to extract
 */
@MessageData.Tag("speech:extract-data")
@MessageData.RequiresResponse
class ExtractData(val speech: String, val type: String) : MessageData {

    constructor(speech: String, type: ArgumentType) : this(speech, type.toString())

}