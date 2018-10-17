package info.deskchan.MessageData.Talk

import info.deskchan.core.MessageData

/**
 * Raise emotion in character
 *
 * @property emotion Emotion name
 * @property force Influence force. 1 by default
 * **/

@MessageData.Tag("talk:make-emotion-influence")
class MakeEmotionInfluence(val emotion: String) : MessageData {

    @MessageData.FieldName("value")
    var force: Float? = null

    constructor(emotion: Emotions) : this(emotion.toString().toLowerCase())

    constructor(emotion: String, force: Float) : this(emotion){
        this.force = force
    }

    constructor(emotion: Emotions, force: Float) : this(emotion){
        this.force = force
    }

    enum class Emotions {
        HAPPINESS, SORROW, FUN, ANGER, CONFUSION, AFFECTION
    }
}