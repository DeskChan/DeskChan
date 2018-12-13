package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set character position.
 *
 * @property top Pixels from top border of screen
 * @property left Pixels from left border of screen
 * @property bottom Pixels from bottom border of screen
 * @property right Pixels from right border of screen
 * @property verticalPercent Vertical position by percents
 * @property horizontalPercent Horizintal position by percents
 **/
@MessageData.Tag("gui:set-character-position")
class SetCharacterPosition : MessageData {

    var top: Int? = null
    var left: Int? = null
    var right: Int? = null
    var bottom: Int? = null
    var verticalPercent: Float? = null
    var horizontalPercent: Float? = null

}