package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Register new command
 *
 * @property tag
 * @property info Reference about what command does, will be visible in GUI. Null by default.
 * @property msgInfo Reference about command parameters format, will be visible in GUI.
 */
@MessageData.Tag("core:add-command")
class AddCommand : MessageData {

    /** Command tag **/
    val tag: String

    /** Reference about what command does, will be visible in GUI. Optional **/
    var info: String? = null

    /** Reference about command parameters format, will be visible in GUI. Optional
     * Should be only String or Map<String, String> **/
    private var msgInfo: Any? = null

    constructor(tag: String) {
        this.tag = tag
    }

    constructor(tag: String, info: String?) : this(tag) {
        this.info = info
    }
    constructor(tag: String, info: String?, msgInfo: String) : this(tag, info){
        this.msgInfo = msgInfo
    }

    constructor(tag: String, info: String?, msgInfo: Map<String, String>) : this(tag, info){
        this.msgInfo = msgInfo
    }

    /** Get reference about command parameters format. **/
    fun getMessageInfo() = msgInfo

    /** Set reference about command parameters format, will be visible in GUI. **/
    fun setMessageInfo(msgInfo: String?){
        this.msgInfo = msgInfo
    }

    /** Set reference about command parameters format, will be visible in GUI. **/
    fun setMessageInfo(msgInfo: Map<String, String>?){
        this.msgInfo = msgInfo
    }
}