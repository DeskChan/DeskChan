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

    /** Set alternative */
    fun setAlternative(srcTag: String, dstTag: String, priority:Int)

    /** Delete alternative */
    fun deleteAlternative(srcTag: String, dstTag: String)

    /** Call next alternative in the chain. */
    fun callNextAlternative(sender: String, tag: String, currentAlternative: String, data: Any?)

    /** Check if message sender is asking for advice. */
    fun isAskingAnswer(sender: String) : Boolean

    /** Set timer.
     * @param delay Delay, ms
     * @param responseListener Function that will be called after delay
     * @return Id of timer  **/
    fun setTimer(delay: Long, responseListener: ResponseListener) : Int

    /** Set cycled timer.
     * @param delay Delay, ms
     * @param count Count of cycles, -1 to infinite
     * @param responseListener Function that will be called after delay
     * @return Id of timer  **/
    fun setTimer(delay: Long, count: Int, responseListener: ResponseListener) : Int

    /** Cancel timer by id. **/
    fun cancelTimer(id: Int)

    /** Properties of plugin **/
    fun getProperties() : PluginProperties

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

    /** Use this to get path to 'DeskChan' directory. **/
    val rootDirPath: Path

    /** Use this to get path where plugin should store temporary user data. **/
    val dataDirPath: Path

    /** Use this to get path to 'assets' folder, where stored program assets. **/
    val assetsDirPath: Path

    /** Use this to get path to 'plugins/pluginId' folder, where stored plugin assets. **/
    val pluginDirPath: Path

    /** Log text to file and console. **/
    fun log(text: String)

    /** Log stack and text of error thrown to file and console. **/
    fun log(e: Throwable)
    
    /** Log text to file and console.
     * @param text
     * @param level
     */
    fun log(text: String,level: LoggerLevel)
}


