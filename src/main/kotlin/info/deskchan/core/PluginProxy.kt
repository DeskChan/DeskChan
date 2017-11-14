package info.deskchan.core

import org.apache.commons.io.FilenameUtils.removeExtension
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import java.util.Locale
import kotlin.collections.HashMap


class PluginProxy (private val id:String, private val plugin: Plugin, private val config: PluginConfig)
    : PluginProxyInterface {

    private val loader: ClassLoader = plugin::class.java.classLoader
    private val messageListeners = HashMap<String, MutableSet<MessageListener>>()
    private val responseListeners  = HashMap<Any, ResponseInfo>()
    private var seq = 0

    override fun getId() : String = id

    fun isNameMatched(name: String) = name == getId() ||
            name == removeExtension(getId()) ||
            removeExtension(name) == getId()

    fun initialize(): Boolean {
        addMessageListener(id, this)
        return plugin.initialize(this)
    }

    override fun unload() {
        try {
            plugin.unload()
        } catch (e: Throwable) {
            log(e)
        }

        for ((key, value) in messageListeners) {
            for (listener in value) {
                PluginManager.getInstance().unregisterMessageListener(key, listener)
            }
        }
        messageListeners.clear()
        PluginManager.getInstance().unregisterPlugin(this)
    }

    override fun sendMessage(tag: String, data: Any?) {
        PluginManager.getInstance().sendMessage(id, tag, data)
    }

    override fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener, returnListener: ResponseListener): Any {
        var data = data?: HashMap<String,Any>()
        if (data !is Map<*, *>) {
            val m = HashMap<String, Any>()
            m.put("data", data)
            data = m
        }
        val m = data as MutableMap<String, Any>
        var seq: Any? = m["seq"]
        if (seq == null)
            seq = this.seq++


        m["seq"] = seq
        val count = PluginManager.getInstance().getMessageListenersCount(tag)
        if (count > 0)
            responseListeners.put(seq, ResponseInfo(responseListener, count, returnListener))
        else
            returnListener.handle(id, null)
        sendMessage(tag, data)
        return seq
    }

    override fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener): Any {
        var data = data?: HashMap<String,Any>()
        if (data !is Map<*, *>) {
            val m = HashMap<String, Any>()
            m.put("data", data)
            data = m
        }
        val m = data as MutableMap<String, Any>
        var seq: Any? = m["seq"]
        if (seq == null) {
            seq = this.seq++
        }
        m["seq"] = seq
        val count = PluginManager.getInstance().getMessageListenersCount(tag)
        if (count > 0)
            responseListeners.put(seq, ResponseInfo(responseListener, count))
        sendMessage(tag, data)
        return seq
    }

    override fun addMessageListener(tag: String, listener: MessageListener) {
        var listeners: MutableSet<MessageListener>? = messageListeners[tag]
        if (listeners == null) {
            listeners = HashSet<MessageListener>()
            messageListeners.put(tag, listeners)
        }
        listeners.add(listener)
        PluginManager.getInstance().registerMessageListener(tag, listener)
    }

    override fun removeMessageListener(tag: String, listener: MessageListener) {
        PluginManager.getInstance().unregisterMessageListener(tag, listener)
        val listeners = messageListeners[tag]
        if (listeners != null) {
            listeners.remove(listener)
            if (listeners.size == 0) {
                messageListeners.remove(tag)
            }
        }
    }

    override fun handleMessage(sender: String, tag: String, data: Any) {
        if (data is Map<*, *>) {
            val m = data as Map<String, Any>
            val seq = m["seq"] ?: return
            val listener = responseListeners[seq] ?: return
            if (listener.handle(sender, id, data)) {
                responseListeners.remove(seq)
            }
        }
    }

    private var plugin_strings: ResourceBundle? = null

    override fun setResourceBundle(path: String) {
        try {
            plugin_strings = ResourceBundle.getBundle(path, Locale.getDefault(), loader)
            log("Loaded bundle for ${id} at ${path}")
        } catch (e: Exception) {
            try{
                val urls = arrayOf<URL>(File(path).toURI().toURL())
                val file_loader = URLClassLoader(urls)
                plugin_strings = ResourceBundle.getBundle("strings", Locale.getDefault(), file_loader)
            } catch (e: Exception) {
                log("Cannot find strings bundle for ${id} at ${path}")
            }
        }
    }

    override fun setConfigField(key: String, value: Any){
        config.append(key,value)
    }

    override fun getConfigField(key: String) = config.get(key)


    override fun getString(key: String): String {
        var s = key
        if (plugin_strings != null && plugin_strings!!.containsKey(key))
            s = plugin_strings!!.getString(key)
        else if (general_strings != null && general_strings!!.containsKey(key))
            s = general_strings!!.getString(key)
        try {
            return java.lang.String(s.toByteArray(charset("ISO-8859-1")), "UTF-8").toString()
        } catch (e: Exception) {
            return s
        }
    }

    fun getConfig() = config

    override val rootDirPath: Path
        get() = PluginManager.getRootDirPath()

    override val dataDirPath: Path
        get() = PluginManager.getPluginDataDirPath(id)

    override val assetsDirPath: Path
        get() = PluginManager.getAssetsDirPath()

    override fun log(text: String) {
        PluginManager.log(id, text)
    }

    override fun log(e: Throwable) {
        PluginManager.log(id, e)
    }

    companion object {
        fun create(plugin: Plugin, id:String, config: PluginConfigInterface?): PluginProxy? {
            val entity = PluginProxy(id, plugin, (config as PluginConfig?) ?: PluginConfig())
            if(!entity.resolveDependencies())
                return null

            val resources = config?.get("resources")
            if(resources!=null)
                entity.setResourceBundle(resources.toString())

            if(!entity.initialize())
                return null

            return entity

        }
        private var general_strings: ResourceBundle? = null

        fun updateResourceBundle() {
            try {
                general_strings = ResourceBundle.getBundle("info/deskchan/strings", Locale.getDefault())
            } catch (e: Exception) {
                PluginManager.log("Cannot find resource bundle info/deskchan/strings")
            }
        }

        init {
            updateResourceBundle()
        }

        fun getString(key: String): String {
            var s = key
            if (general_strings != null && general_strings!!.containsKey(key))
                s = general_strings!!.getString(key)

            try {
                return java.lang.String(s.toByteArray(charset("ISO-8859-1")), "UTF-8").toString()
            } catch (e: Exception) {
                return s
            }
        }
    }

    // TODO: Implement dependencies resolving from remote repositories.
    // TODO: Or implement a way to delegate resolving to another plugin.
    fun resolveDependencies() : Boolean {
        config.getDependencies().forEach {
            val r = PluginManager.getInstance().tryLoadPluginByName(it)
            if (!r){
                PluginManager.log("Failed to load dependency $it of plugin ${getId()}")
                return false
            }
        }
        return true
    }
}

internal class ResponseInfo {
    private var count: Int = 0
    private var res: ResponseListener? = null
    private var ret: ResponseListener? = null

    constructor(responseListener: ResponseListener, count: Int) {
        this.count = count
        res = responseListener
        ret = null
    }

    constructor(responseListener: ResponseListener, count: Int, returnListener: ResponseListener) {
        this.count = count
        res = responseListener
        ret = returnListener
    }

    fun handle(sender: String, id: String, data: Any): Boolean {
        res!!.handle(sender, data)
        count--
        if (count == 0) {
            if (ret != null) ret!!.handle(id, null)
            return true
        }
        return false
    }

}
