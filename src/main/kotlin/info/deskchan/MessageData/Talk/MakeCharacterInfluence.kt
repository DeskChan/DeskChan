package info.deskchan.MessageData.Talk

import info.deskchan.core.MessageData

/**
 * Make influence on character features
 *
 * @property feature Feature name
 * @property force Influence strength. 1 by default
 * **/

@MessageData.Tag("talk:make-character-influence")
class MakeCharacterInfluence(val feature: String) : MessageData {

    @MessageData.FieldName("value")
    var force: Float? = null

    constructor(feature: Features) : this(feature.toString().toLowerCase())

    constructor(feature: String, force: Float) : this(feature){
        this.force = force
    }

    constructor(feature: Features, force: Float) : this(feature){
        this.force = force
    }

    enum class Features {
        EMPATHY, IMPULSIVITY, SELFCONFIDENCE, ENERGY, ATTITUDE, EXPERIENCE, MANNER, RELATIONSHIP
    }
}