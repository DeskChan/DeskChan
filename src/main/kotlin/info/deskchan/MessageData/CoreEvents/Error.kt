package info.deskchan.MessageData.CoreEvents

import info.deskchan.core.MessageData

/**
 * Called every time some plugin sends error
 *
 */
@MessageData.Tag("core-events:log")
class Error : MessageData {

    @MessageData.FieldName("class")
    val className: String

    val message: String?
    val traceback: List<Any>?

    constructor(className: String, message: String?, traceback: List<Any>?){
        this.className = className
        this.message = message
        this.traceback = traceback
    }

    constructor(className: String) : this(className, null, null)
}