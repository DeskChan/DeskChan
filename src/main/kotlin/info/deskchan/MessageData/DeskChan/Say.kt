package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Say phrase by the character
 *
 * @property text Text to say
 * @property characterImage Emotion sprite name for character to use when saying text
 * @property priority Priority for message
 * @property skippable If priority is lower than phrases in current queue, message can be skipped or not. True, by default
 * @property timeout If there is such possibility, text will be shown exactly this time. Count in ms.
 * @property partible If there is need, text will be split to parts to fit in  GUI window, if true. True by default
 */
@MessageData.Tag("DeskChan:say")
class Say (val text: String) : MessageData {

    var characterImage: String? = null
    var partible: Boolean? = null
    var skippable: Boolean? = null

    var priority: Long? = null
        set(value){
            priority = java.lang.Long.max(value?: 0L, 0L)
        }


    var timeout: Long? = null
        set(value) {
            timeout = when (value){
                null -> null
                else -> {
                    if (value < 50) null
                    else value
                }
            }
        }


    constructor(text: String, characterImage: String) : this(text) {
        this.characterImage = characterImage
    }

    constructor(text: String, characterImage: Sprite) : this(text) {
        this.characterImage = characterImage.toString()
    }

    fun setCharacterImage(characterImage: Sprite){
        this.characterImage = characterImage.toString()
    }

    enum class Sprite {
        NORMAL, SCEPTIC, DESPAIR, MAD, SMILE, THOUGHTFUL, EXCITEMENT,
        LAUGH, HOPE, VULGAR, SHY, SCARED, CRY, SAD, ANGRY, RAGE, HAPPY,
        DISGUSTED, SORRY, GRIN, TIRED, CONFIDENT, LOVE, BOUNTY, CONFUSED,
        SHOCKED, CURIOUS, WAITING, ERROR, OFFENDED, SURPRISED
    }
}