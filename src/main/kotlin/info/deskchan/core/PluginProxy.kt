package info.deskchan.core

import org.apache.commons.io.FilenameUtils.removeExtension
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
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
    private val typedListeners = HashMap<TypedMessageListener<*>, MessageListener>()
    private val responseListeners = HashMap<Any, ResponseInfo>()
    private var seq = 0
    private val properties: PluginProperties = PluginProperties(this)

    override fun getId(): String = id

    fun isNameMatched(name: String) = name == getId() ||
            name == removeExtension(getId()) ||
            removeExtension(name) == getId()

    fun initialize(): Boolean {
        addMessageListener(id+"#", this)
        addMessageListener(id+":save-properties", MessageListener { sender, tag, data -> properties.save() })
        config.append("id", id)
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
        typedListeners.clear()
        PluginManager.getInstance().unregisterPlugin(this)
    }

    override fun sendMessage(tag: String, data: Any?) {
        PluginManager.getInstance().sendMessage(id, tag, data)
    }

    override fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener, returnListener: ResponseListener): Any {
        val count = PluginManager.getInstance().getMessageListenersCount(tag)
        // if there is no listeners for this tag, we skip sending message
        if (count == 0) {
            returnListener.handle(id, null)
            return -1
        }

        seq++
        responseListeners.put(seq, ResponseInfo(responseListener, count, returnListener))
        PluginManager.getInstance().sendMessage(id + "#" + seq, tag, data)
        return seq
    }

    override fun sendMessage(tag: String, data: Any?, responseListener: ResponseListener): Any {
        val count = PluginManager.getInstance().getMessageListenersCount(tag)
        // if there is no listeners for this tag, we skip sending message
        if (count == 0)
            return -1

        seq++
        responseListeners.put(seq, ResponseInfo(responseListener, count))
        PluginManager.getInstance().sendMessage(id + "#" + seq, tag, data)
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

    override fun <T> addTypedMessageListener(tag: String, listener: TypedMessageListener<T>) {
        for (method in listener.javaClass.methods) {
            if ((method.name == "handleMessage") && (method.parameterCount == 3)) {
                val cls = method.parameterTypes.last()
                val l = MessageListener { sender, tag, data ->
                    listener.handleMessage(sender, tag,
                            MessageDataUtils.deserialize(data as Map<String, Object>, cls) as T)
                }
                addMessageListener(tag, l)
                typedListeners[listener] = l
                break
            }
        }
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

    override fun <T> removeTypedMessageListener(tag: String, listener: TypedMessageListener<T>) {
        val l = typedListeners.remove(listener)
        if (l != null) {
            removeMessageListener(tag, l)
        }
    }

    // Alternatives

    override fun setAlternative(srcTag: String, dstTag: String, priority:Int) {
        Alternatives.registerAlternative(srcTag, dstTag, id, priority)
    }

    override fun deleteAlternative(srcTag: String, dstTag: String) {
        Alternatives.unregisterAlternative(srcTag, dstTag, id)
    }

    override fun callNextAlternative(sender: String, tag: String, currentAlternative: String, data: Any?) {
        Alternatives.callNextAlternative(sender, tag, currentAlternative, data)
    }

    override fun isAskingAnswer(sender: String) : Boolean {
        return sender.contains('#')
    }

    // Other

    override fun handleMessage(sender: String, tag: String, data: Any?) {
        val delimiterPas = tag.indexOf('#')
        if (delimiterPas >= 0 && tag.startsWith(id)) {
            val seq = Integer.parseInt(tag.substring(delimiterPas + 1))
            val listener = responseListeners[seq] ?: return
            if (listener.handle(sender, id, data)) {
                responseListeners.remove(seq)
            }
        }
    }

    override fun setTimer(delay: Long, responseListener: ResponseListener): Int {
        return CoreTimerTask(delay, 1, responseListener).hashCode()
    }

    override fun setTimer(delay: Long, count: Int, responseListener: ResponseListener): Int {
        return CoreTimerTask(delay, count, responseListener).hashCode()
    }

    override fun cancelTimer(id: Int) {
        timers.forEach {
            if (it.hashCode() == id) {
                it.stop()
                return
            }
        }
    }

    override fun getProperties() = properties

    private var plugin_strings: ResourceBundle? = null

    override fun setResourceBundle(path: String) {
        try {
            plugin_strings = ResourceBundle.getBundle(path, Locale.getDefault(), loader, UTF8Control())
            log("Loaded bundle for ${id} at ${path}")
        } catch (e: Exception) {
            try {
                val urls = arrayOf<URL>(File(path).toURI().toURL())
                val file_loader = URLClassLoader(urls)
                plugin_strings = ResourceBundle.getBundle("strings", Locale.getDefault(), file_loader, UTF8Control())
            } catch (e: Exception) {
                log("Cannot find strings bundle for ${id} at ${path}, using existing resources")
            }
        }
    }

    override fun setConfigField(key: String, value: Any) {
        config.append(key, value)
    }

    override fun getConfigField(key: String) = config.get(key)


    override fun getString(key: String): String {
        var s = key
        if (plugin_strings != null && plugin_strings!!.containsKey(key))
            s = plugin_strings!!.getString(key)
        else if (general_strings != null && general_strings!!.containsKey(key))
            s = general_strings!!.getString(key)
        return s
    }

    fun getConfig() = config

    override val rootDirPath: Path
        get() = PluginManager.getRootDirPath()

    override val dataDirPath: Path
        get() = PluginManager.getPluginDataDirPath(id)

    override val pluginDirPath: Path
        get() = PluginManager.getDefaultPluginDirPath(id)

    override val assetsDirPath: Path
        get() = PluginManager.getAssetsDirPath()

    override fun log(text: String) {
        sendMessage("core-events:log", mapOf("message" to text))
    }

    override fun log(text: String, level: LoggerLevel) {
        sendMessage("core-events:log", mapOf("message" to text,"level" to level))
    }

    override fun log(e: Throwable) {
        var t: Throwable? = e
        while (t?.cause != null) t = t.cause

        val stacktrace = mutableListOf<String>()
        t?.stackTrace?.forEach { stacktrace.add(it.toString()) }

        sendMessage("core-events:error", mapOf("class" to t?.javaClass?.simpleName, "message" to t?.message, "stacktrace" to stacktrace))
    }

    companion object {
        @Throws(Exception::class)
        fun create(plugin: Plugin, id: String, config: PluginConfig?): PluginProxy? {
            val entity = PluginProxy(id, plugin, config ?: PluginConfig())
            if (!entity.resolveDependencies())
                return null

            val resources = config?.get("resources")
            if (resources != null)
                entity.setResourceBundle(resources.toString())

            if (!entity.initialize())
                return null

            return entity

        }

        private var general_strings: ResourceBundle? = null

        @Throws(Exception::class)
        fun updateResourceBundle(){
            general_strings = ResourceBundle.getBundle("info/deskchan/strings", UTF8Control())
        }

        fun getString(key: String): String {
            var s = key
            if (general_strings != null && general_strings!!.containsKey(key))
                s = general_strings!!.getString(key)

            return s
        }
    }

    // TODO: Implement dependencies resolving from remote repositories.
    // TODO: Or implement a way to delegate resolving to another plugin.
    fun resolveDependencies(): Boolean {
        config.getDependencies().forEach {
            val r = PluginManager.getInstance().tryLoadPluginByName(it)
            if (!r) {
                log(Exception("Failed to load dependency $it of plugin ${getId()}"))
                return false
            }
        }
        return true
    }

    /* Timers */

    protected var timers: MutableList<CoreTimerTask> = mutableListOf()

    inner class CoreTimerTask(val delay: Long, var count: Int, val response: ResponseListener) : ResponseListener, Runnable {

        protected var lastSeq: Any? = null

        init {
            timers.add(this)
            start()
        }

        override fun handle(sender: String, data: Any?) {
            run()
            if (count > 0) count--
            if (count == 0) return
            lastSeq = null
            start()
        }

        fun start() {
            if (lastSeq != null) stop()
            lastSeq = sendMessage("core-utils:notify-after-delay", mapOf("delay" to delay), this)
        }

        fun stop() {
            if (lastSeq != null)
                sendMessage("core-utils:notify-after-delay", mapOf("cancel" to lastSeq))
            timers.remove(this)
        }

        override fun run() {
            response.handle(getId(), null)
        }
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

    fun handle(sender: String, id: String, data: Any?): Boolean {
        res!!.handle(sender, data)
        count--
        if (count == 0) {
            if (ret != null) ret!!.handle(id, null)
            return true
        }
        return false
    }
}

// Override getBundle to correctly read bundles in UTF-8
internal class UTF8Control : ResourceBundle.Control() {
    @Throws(IllegalAccessException::class, InstantiationException::class, IOException::class)
    override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle? {
        // The below is a copy of the default implementation.
        val bundleName = toBundleName(baseName, locale)
        val resourceName = toResourceName(bundleName, "properties")
        var bundle: ResourceBundle? = null
        var stream: InputStream? = null
        if (reload) {
            val url = loader.getResource(resourceName)
            if (url != null) {
                val connection = url.openConnection()
                if (connection != null) {
                    connection.useCaches = false
                    stream = connection.getInputStream()
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName)
        }
        if (stream != null) {
            try {
                // Only this line is changed to make it to read properties files as UTF-8.
                bundle = PropertyResourceBundle(InputStreamReader(stream, "UTF-8"))
            } finally {
                stream!!.close()
            }
        }
        return bundle
    }
}