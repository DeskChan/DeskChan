package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set balloon position mode. Balloon position modes can be different on each platform, so no enum for you.
 **/
@MessageData.Tag("gui:change-balloon-direction-mode")
class ChangeBalloonPositionMode(val value: String) : MessageData