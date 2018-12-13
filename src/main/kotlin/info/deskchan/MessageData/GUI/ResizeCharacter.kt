package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Resize character.
 *
 * @property scaleFactor Absolute scaling value, float percents
 * @property zoom Scaling value relative from current, float percents
 * @property width Width of image
 * @property height Height of image
 **/
@MessageData.Tag("gui:resize-character")
class ResizeCharacter : MessageData {

    private val scaleFactor: Float?
    private val zoom: Float?
    private val width: Int?
    private val height: Int?

    private constructor(scaleFactor: Float?, zoom: Float?, width: Int?, height: Int?){
        this.scaleFactor = scaleFactor
        this.zoom = zoom
        this.width = width
        this.height = height
    }

    companion object {
        fun absoluteScale (value: Float) = ResizeCharacter(value, null, null, null)
        fun relativeScale (value: Float) = ResizeCharacter(null, value, null, null)
        fun exactSize (width: Int?, height: Int?) {
            if (width == null && height == null)
                throw RuntimeException("You should set at least width or height");
            ResizeCharacter(null, null, width, height)
        }
    }
}