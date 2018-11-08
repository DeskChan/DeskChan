package info.deskchan.MessageData.GUIEvents

import info.deskchan.core.MessageData

/**
 * Called every time user perform middle button scroll on balloon
 *
 * @property screenX Click X position, absolute (relative of screen)
 * @property screenY Click Y position, absolute (relative of screen)
 * @property nodeX Click X position, relative of character sprite
 * @property nodeY Click Y position, relative of character sprite
 * @property delta direction of scrolling (up = -1, down = 1)
 */
@MessageData.Tag("gui-events:balloon-scroll")
class BalloonScroll(val screenX: Int, val screenY: Int, val nodeX: Int, val nodeY: Int, val delta: Int) : MessageData