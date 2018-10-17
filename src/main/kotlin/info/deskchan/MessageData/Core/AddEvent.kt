package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Register new event
 *
 * @property tag Event tag
 * @property info Reference about when event called, will be visible in GUI. Optional
 * @property ruleInfo Reference about event rule format, will be visible in GUI. Optional
 */
@MessageData.Tag("core:add-event")
class AddEvent(val tag: String, var info: String?, var ruleInfo: String?) : MessageData {

    constructor(tag: String) : this(tag, null, null)

}