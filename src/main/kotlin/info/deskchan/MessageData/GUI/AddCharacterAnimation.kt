package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Add animation to character
 *
 * @property animations Animations list
 **/
@MessageData.Tag("gui:add-character-animation")
class AddCharacterAnimation(val animations: List<SetSprite.Animation>) : MessageData