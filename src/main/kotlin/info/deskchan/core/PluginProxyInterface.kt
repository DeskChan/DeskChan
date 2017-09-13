package info.deskchan.core

import java.nio.file.Path

interface PluginProxyInterface : MessageListener {

    fun getId() : String

    fun unload()

    fun sendMessage(tag: String, data: Any?)

    fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener, returnListener: ResponseListener): Any

    fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener): Any

    fun addMessageListener(tag: String, listener: MessageListener)

    fun removeMessageListener(tag: String, listener: MessageListener)

    fun setResourceBundle(path: String)

    fun setConfigField(key: String, value: Any)

    fun getConfigField(key:String): Any?

    fun getString(key: String): String

    val rootDirPath: Path

    val dataDirPath: Path

    val assetsDirPath: Path

    fun log(text: String)

    fun log(e: Throwable)
}


