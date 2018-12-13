package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set program layer mode. Layer modes can be different on each platform, so no enum for you.
 **/
@MessageData.Tag("gui:change-layer-mode")
class ChangeLayerMode(val value: String) : MessageData