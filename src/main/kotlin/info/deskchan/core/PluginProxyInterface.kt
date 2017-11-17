package info.deskchan.core

import java.nio.file.Path

interface PluginProxyInterface : MessageListener {

    /** Get name of plugin. **/
    fun getId() : String

    /** Unload plugin from program immediately. This method is usually called from core. **/
    fun unload()

    /** Send message at tag through core.
     * @param tag Tag
     * @param data Any data that will be sent with message, can be null
     */
    fun sendMessage(tag: String, data: Any?)

    /** Send message at tag through core. First listener will be called if receiver wants to response to your message.
     * Second listener will be called after all of receivers get your message and complete their work with it.
     * @param tag Tag
     * @param data Any data that will be sent with message, can be null
     * @param responseListener Response to your message
     * @param returnListener Listener tat will be called after all of receivers get your message
     */
    fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener, returnListener: ResponseListener): Any

    /** Send message at tag through core. Listener will be called if receiver wants to response to your message.
     * @param tag Tag
     * @param data Any data that will be sent with message, can be null
     * @param responseListener Response to your message
     */
    fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener): Any

    /** Add listener to tag. All messages from everywhere in program will be received by this listener. */
    fun addMessageListener(tag: String, listener: MessageListener)

    /** Remove listener to tag. */
    fun removeMessageListener(tag: String, listener: MessageListener)

    /** Set path to resource bundle that you want to be used by your plugin.
     * @param path Path to resources folder
     */
    fun setResourceBundle(path: String)

    /** Set config field of your plugin. */
    fun setConfigField(key: String, value: Any)

    /** Get config field of your plugin. */
    fun getConfigField(key:String): Any?

    /** Get resource string from resource bundles. Resources will be searched not only in bundle that you specified
     * but also in main bundle. */
    fun getString(key: String): String

    val rootDirPath: Path

    val dataDirPath: Path

    val assetsDirPath: Path

    /** Log text to file and console. **/
    fun log(text: String)

    /** Log stack and text of error thrown to file and console. **/
    fun log(e: Throwable)
}


