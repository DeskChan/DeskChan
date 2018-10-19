package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Set sprite state. You can show your own custom sprites and animate them.
 *
 * @property id System id of sprite (will be transformed to "sender-id")
 * @property type Type of action to perform towards sprite, CREATE by default
 *
 * @property animations Character animations
 * @property posX X coord of sprite, in pixels
 * @property posY Y coord of sprite, in pixels
 * @property scaleX Horizontal scale of sprite
 * @property scaleY Vertical scale of sprite
 * @property rotation Rotation of sprite
 * @property draggable Can be dragged, True by default
 **/
@MessageData.Tag("gui:add-character-animation")
class SetSprite : MessageData {

    enum class SpriteActionType {
        CREATE, SHOW, HIDE, DELETE, ANIMATE, DROP_ANIMATION
    }

    val id: String
    private var type: String = "CREATE"

    var posX: Int? = null
    var posY: Int? = null
    var scaleX: Float? = null
    var scaleY: Float? = null
    var rotation: Float? = null
    var draggable: Boolean? = null

    var animations: List<Animation>? = null

    fun getActionType() = SpriteActionType.valueOf(type.toUpperCase())

    fun setActionType(value: SpriteActionType){
        type = value.toString()
    }

    constructor(id: String, type: SpriteActionType){
        this.id = id
        setActionType(type)
    }

    open class Animation : MessageData {
        var next: Path? = null
        var scalingX: Float? = null
            get() = field?: 0F
        var scalingY: Float? = null
            get() = field?: 0F
        var movingX: Float? = null
            get() = field?: 0F
        var movingY: Float? = null
            get() = field?: 0F
//      var movingZ = 0f
        var rotation: Float? = null
            get() = field?: 0F
        var smooth: Boolean? = null
            get() = field?: false
        var opacity: Float? = null
            get() = field?: 0F
        var delay: Long? = null
            get() = field?: 200L
    }
}