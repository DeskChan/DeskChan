package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Event-command link
 *
 * @property tag Command tag
 * @property rule Rule, string in format specified by event
 * @property msgData Any-type object, will be sent to command as data when event will be triggered
 */
open class EventLink(val tag: String, var rule: String?, var msgData: Any?) : MessageData {

    constructor(commandName: String) : this(commandName, null, null)

    constructor(commandName: String, rule: String?) : this(commandName, rule, null)

}
