package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Play sound.
 *
 * @property file Sound file
 * @property volume Volume to file
 **/
@MessageData.Tag("gui:play-sound")
class PlaySound(val file: String) : MessageData {

    var volume: Int? = null

    constructor(file: Path) : this(file.absolutePath)

    constructor(file: String, volume: Int) : this(file){
        this.volume = minOf(maxOf(volume, 0), 100)
    }

    constructor(file: Path, volume: Int) : this(file){
        this.volume = minOf(maxOf(volume, 0), 100)
    }

}