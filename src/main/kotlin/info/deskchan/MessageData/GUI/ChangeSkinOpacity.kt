package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Change skin opacity.
 *
 * @property absolute Absolute value, float percent
 * @property relative Value relative from current, float percent
 **/
@MessageData.Tag("gui:change-skin-opacity")
class ChangeSkinOpacity : MessageData {

    private val absolute: Float?
    private val relative: Float?

    private constructor(absolute: Float?, relative: Float?){
        this.absolute = absolute
        this.relative = relative
    }

    companion object {
        fun absolute (value: Float) = ChangeSkinOpacity(value, null)
        fun relative (value: Float) = ChangeSkinOpacity(null, value)
    }
}