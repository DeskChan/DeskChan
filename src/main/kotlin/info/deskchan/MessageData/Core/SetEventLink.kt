package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Register new event-command link
 *
 * @property eventName Event tag
 * @property commandName Command tag
 * @property rule Rule, string in format specified by event
 * @property msgData Any-type object, will be sent to command as data when event will be triggered
 * @property isDefault Is event will be registered by default. If true, event will be recovered after links resetting. True by default
 */
@MessageData.Tag("core:set-event-link")
class SetEventLink(val eventName: String, val commandName: String, var rule: String?, var msgData: Any?, var isDefault: Boolean) : MessageData {

    constructor(eventName: String, commandName: String) : this(eventName, commandName, null, null, true)

    constructor(eventName: String, commandName: String, rule: String?) : this(eventName, commandName, rule, null, true)

    constructor(eventName: String, commandName: String, rule: String?, msgData: Any?) : this(eventName, commandName, rule, msgData, true)

}
