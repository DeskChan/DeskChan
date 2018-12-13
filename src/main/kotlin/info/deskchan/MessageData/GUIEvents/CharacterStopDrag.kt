package info.deskchan.MessageData.GUIEvents

import info.deskchan.core.MessageData

/**
 * Called every time user stops to drag character
 *
 * @property screenX Click X position, relative of screen
 * @property screenY Click Y position, relative of screen
 */
@MessageData.Tag("gui-events:character-stop-drag")
class CharacterStopDrag(val screenX: Int, val screenY: Int) : MessageData