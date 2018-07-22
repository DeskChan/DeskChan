package info.deskchan.groovy_support

import groovy.lang.Script
import info.deskchan.core.*
import java.nio.file.Path
import java.util.*

abstract class GroovyPlugin : Script(), Plugin {

    private var pluginProxy: PluginProxyInterface? = null
    private val cleanupHandlers = ArrayList<Runnable>()

    var pluginDirPath: Path? = null
        get() = pluginProxy!!.pluginDirPath
    var assetsDirPath: Path? = null
        get() = pluginProxy!!.assetsDirPath
    var rootDirPath: Path? = null
        get() = pluginProxy!!.rootDirPath
    val dataDirPath: Path
        get() = pluginProxy!!.dataDirPath

    val id: String
        get() = pluginProxy!!.getId()

    val properties: PluginProperties
        get() = pluginProxy!!.getProperties()


    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {
        this.pluginProxy = pluginProxy
        try {
            run()
        } catch (e: Exception) {
            pluginProxy.log(e)
            return false
        }

        return true
    }

    override fun unload() {
        for (runnable in cleanupHandlers) {
            runnable.run()
        }
    }

    fun sendMessage(tag: String, data: Any?) {
        pluginProxy!!.sendMessage(tag, data)
    }

    fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener) {
        pluginProxy!!.sendMessage(tag, data, responseListener)
    }

    fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener, returnListener: ResponseListener) {
        pluginProxy!!.sendMessage(tag, data, responseListener, returnListener)
    }

    fun addMessageListener(tag: String, listener: MessageListener) {
        pluginProxy!!.addMessageListener(tag, listener)
    }

    fun removeMessageListener(tag: String, listener: MessageListener) {
        pluginProxy!!.removeMessageListener(tag, listener)
    }

     fun setTimer(delay: Long, listener: ResponseListener): Int {
        return pluginProxy!!.setTimer(delay, listener)
    }

    fun setTimer(delay: Long, count: Int, listener: ResponseListener): Int {
        return pluginProxy!!.setTimer(delay, count, listener)
    }

    fun cancelTimer(id: Int) {
        pluginProxy!!.cancelTimer(id)
    }

    fun getString(key: String): String {
        return pluginProxy!!.getString(key)
    }

    fun addCleanupHandler(handler: Runnable) {
        cleanupHandlers.add(handler)
    }

    fun setResourceBundle(path: String) {
        pluginProxy!!.setResourceBundle(pluginDirPath!!.resolve(path).toString())
    }

    fun setConfigField(key: String, value: Any) {
        pluginProxy!!.setConfigField(key, value)
    }

    fun getConfigField(key: String): Any? {
        return pluginProxy!!.getConfigField(key)
    }

    fun log(text: String) {
        pluginProxy!!.log(text)
    }

    fun log(e: Throwable) {
        pluginProxy!!.log(e)
    }

    fun setAlternative(srcTag: String, dstTag: String, priority:Int) {
        pluginProxy!!.setAlternative(srcTag, dstTag, priority)
    }

    fun deleteAlternative(srcTag: String, dstTag: String) {
        pluginProxy!!.deleteAlternative(srcTag, dstTag)
    }

    fun callNextAlternative(sender: String, tag: String, currentAlternative: String, data: Any?) {
        pluginProxy!!.callNextAlternative(sender, tag, currentAlternative, data)
    }

}
