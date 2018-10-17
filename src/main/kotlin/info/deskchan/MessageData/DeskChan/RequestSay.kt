package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Say phrase by the character with specific intent
 *
 * If response is requested, phrase will be sended as response. Phrase will be sent to DeskChan:say otherwise.
 *
 * @property intent Intent for phrase. If provided more than one, as much phrases as intents count will be generated.
 * @property characterImage Emotion sprite name for character to use when saying phrase. Sprite will be selected by character system if no name provided.
 * @property priority Priority for message
 */
@MessageData.Tag("DeskChan:request-say")
@MessageData.RequiresResponse
class RequestSay : MessageData {

    private var intent: Any

    fun getIntent() = intent

    /** Set intents list for phrase. As much phrases as intents count will be generated. **/
    fun setIntent(value: Collection<String>) {
        intent = value.toMutableList()
    }

    /** Set intent for phrase. **/
    fun setIntent(value: Any){
        intent = value.toString()
    }

    var characterImage: String? = null

    var priority: Long? = null
        set(value){
            priority = java.lang.Long.max(value?: 0L, 0L)
        }

    constructor(intent: String){
        this.intent = intent
    }

    constructor(intent: String, characterImage: String) : this(intent) {
        this.characterImage = characterImage
    }

    constructor(text: String, characterImage: Say.Sprite) : this(text) {
        this.characterImage = characterImage.toString()
    }

    constructor(intents: Collection<String>){
        this.intent = intents
    }

    constructor(intent: Collection<String>, characterImage: String) : this(intent) {
        this.characterImage = characterImage
    }

    constructor(text: Collection<String>, characterImage: Say.Sprite) : this(text) {
        this.characterImage = characterImage.toString()
    }

    fun setCharacterImage(characterImage: Say.Sprite){
        this.characterImage = characterImage.toString()
    }

    companion object {
        val ResponseFormat: Map<String, Any>? = null
    }
}