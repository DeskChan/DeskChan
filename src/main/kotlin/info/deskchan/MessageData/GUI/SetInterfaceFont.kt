package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set current interface font.
 *
 * @property font Font string. Font needs to be provided as string, as example "PT Sans, 14".
 **/
@MessageData.Tag("gui:set-interface-font")
class SetInterfaceFont (val font: String) : MessageData {

    constructor(fontFamily: String, size: Int) : this(fontFamily + ", " + size)

}