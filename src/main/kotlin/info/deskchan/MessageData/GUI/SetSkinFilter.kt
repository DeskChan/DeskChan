package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set skin filter. Don't specify any value to drop current skin filter
 *
 * @property red Red channel
 * @property green Green channel
 * @property blue Blue channel
 * @property opacity Opacity
 **/
@MessageData.Tag("gui:set-skin-filter")
class SetSkinFilter : MessageData {

    private var red: Double? = null
    private var green: Double? = null
    private var blue: Double? = null
    private var opacity: Double? = null

    constructor(red: Double?, green: Double?, blue: Double?, opacity: Double?){
        this.red = red
        this.green = green
        this.blue = blue
        this.opacity = opacity
    }

    constructor()
}