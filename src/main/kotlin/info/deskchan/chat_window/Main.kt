package info.deskchan.chat_window

import info.deskchan.MessageData.Core.AddCommand
import info.deskchan.MessageData.Core.SetEventLink
import info.deskchan.MessageData.DeskChan.RequestSay
import info.deskchan.MessageData.GUI.Control
import info.deskchan.MessageData.GUI.InlineControls
import info.deskchan.MessageData.GUI.SetPanel
import info.deskchan.core.*

import java.text.SimpleDateFormat
import java.util.*


class Main : Plugin {

    lateinit var characterName: String
    lateinit var userName: String

    private var chatIsOpened = false

    private val history = LinkedList<ChatPhrase>()
    private var logLength = 1
    private var currentQuery = ""

    /** Single phrase in chat.  */
    private inner class ChatPhrase(val text: String, val sender: Int) {
        val date: Date
        // DeskChan
        // User
        // Technical messages
        val nameMap: Map<String, Any>
            get() {
                val map = HashMap<String, Any>()
                val color: String
                var senderName: String? = null
                var font: String? = null
                var dateString = "(" + SimpleDateFormat("HH:mm:ss").format(date) + ") "
                when (sender) {
                    0 -> {
                        color = properties.getString("deskchan-color", "#F00")
                        font = properties.getString("deskchan-font")
                        senderName = characterName
                    }
                    1 -> {
                        color = properties.getString("user-color", "#00A")
                        font = properties.getString("user-font")
                        senderName = userName
                    }
                    2 -> {
                        color = "#888"
                        dateString = ""
                    }
                    else -> {
                        color = "#000"
                    }
                }
                map["text"] = dateString + if (senderName != null) "[$senderName]: " else ""
                map["color"] = color
                map["id"] = "name"
                if (font != null) {
                    map["font"] = font
                }
                return map
            }
        val textMap: Map<String, Any>
            get() {
                val map = HashMap<String, Any>()
                map["text"] = text + "\n"
                map["id"] = "text"
                return map
            }

        init {
            date = Date()
        }
    }

    private fun historyToChat(): List<Map<*, *>> {
        val ret = ArrayList<Map<*, *>>()
        val list = history.subList(Math.max(history.size - logLength, 0), history.size)
        if (list.size == 0) {
            ret.add(ChatPhrase(pluginProxy.getString("message.chat_empty"), 2).textMap)
            return ret
        }
        for (phrase in list) {
            ret.add(phrase.nameMap)
            ret.add(phrase.textMap)
        }
        return ret
    }

    override fun initialize(newPluginProxy: PluginProxyInterface): Boolean {
        pluginProxy = newPluginProxy
        log("setup chat window started")
        pluginProxy.setConfigField("name", pluginProxy.getString("plugin-name"))

        // setting default properties
        properties = pluginProxy.getProperties()
        properties.load()
        properties.putIfHasNot("length", 50)
        properties.putIfHasNot("user-color", "#00A")
        properties.putIfHasNot("deskchan-color", "#F00")

        logLength = properties.getInteger("length")!!

        characterName = "DeskChan"
        userName = pluginProxy.getString("default-username")

        /* Open chat request.
        * Public message
        * Params: None
        * Returns: None */
        pluginProxy.addMessageListener("chat:open", MessageListener { sender, tag, data ->
            chatIsOpened = true
            pluginProxy.sendMessage(RequestSay("START_DIALOG"))
            pluginProxy.sendMessage(SetPanel("chat", SetPanel.PanelType.WINDOW, SetPanel.ActionType.SHOW))
        })

        pluginProxy.sendMessage(AddCommand("chat:open"))

        pluginProxy.sendMessage(SetEventLink(
                "gui:keyboard-handle",
                "chat:open",
                "ALT+C"
        ))

        /* Chat has been closed through GUI. */
        pluginProxy.addMessageListener("chat:closed", MessageListener { sender, tag, data -> chatIsOpened = false })

        /* Updated textfield input. */
        pluginProxy.addMessageListener("chat:update-textfield", MessageListener { sender, tag, data -> currentQuery = data.toString() })

        /* Someone made request to clear all messages from chat. We're clearing it. */
        pluginProxy.addMessageListener("chat:clear", MessageListener { sender, tag, data ->
            history.clear()
            setupChat()
        })

        /* Someone made request to change user phrases color in chat.
        * Public message
        * Params: color:String? or null to reset
        * Returns: None */
        pluginProxy.addMessageListener("chat:set-user-color", MessageListener { sender, tag, data ->
            properties["user-color"] = data.toString()
            setupChat()
        })

        /* Someone made request to change user DeskChan color in chat.
        * Public message
        * Params: color:String? or null to reset
        * Returns: None */
        pluginProxy.addMessageListener("chat:set-deskchan-color", MessageListener { sender, tag, data ->
            properties.putIfNotNull("deskchan-color", data.toString())
            setupChat()
        })

        /* Listening all DeskChan speech to show it in chat. */
        pluginProxy.addMessageListener("DeskChan:just-said", MessageListener { sender, tag, data ->
            val map = MessageDataMap("text", data)

            // very small timer to run this func in other thread
            pluginProxy.setTimer(20, ResponseListener { s, d ->
                history.add(ChatPhrase(map.getString("text", ""), 0))

                pluginProxy.sendMessage(SetPanel("chat", SetPanel.PanelType.WINDOW, SetPanel.ActionType.UPDATE,
                        Control(
                                Control.ControlType.CustomizableTextArea,
                                "textarea",
                                historyToChat()
                        )
                ))
            })
        })

        /* Listening all user speech to show it in chat.
        * Technical message
        * Params: value: String! - user speech text
        * Returns: None */
        pluginProxy.addMessageListener("chat:user-said", MessageListener { sender, tag, data ->
            currentQuery = MessageDataMap("value", data).getString("value", currentQuery)

            pluginProxy.sendMessage("DeskChan:user-said", data)
        })

        pluginProxy.addMessageListener("DeskChan:user-said", MessageListener { sender, tag, data ->
            val map = MessageDataMap("value", data)
            history.add(ChatPhrase(map.getString("value", ""), 1))
            setupChat()
        })

        /* Saving options changed in options window.
        * Public message
        * Params:
        *         length: Int? - history length
        *         user-color: String? - user color
        *         deskchan-color: String? - deskchan color
        *         user-font: String? - user font in inner format
        *         deskchan-fontr: String? - deskchan font in inner format
        * Returns: None */
        pluginProxy.addMessageListener("chat:save-options", MessageListener { sender, tag, dat ->
            try {
                val data = dat as Map<*, *>
                properties.putIfNotNull("length", data["length"])
                properties.putIfNotNull("user-color", data["user-color"])
                properties.putIfNotNull("deskchan-color", data["deskchan-color"])
                properties.putIfNotNull("user-font", data["user-font"])
                properties.putIfNotNull("deskchan-font", data["deskchan-font"])

                logLength = properties.getInteger("length", logLength)

                saveOptions()
                setupChat()
                setupOptions()
            } catch (e: Exception) {

            }
        })

        /* Character was updated, so we changing his name and username in chat. */
        pluginProxy.addMessageListener("talk:character-updated", MessageListener { sender, tag, data -> onCharacterUpdate(MessageDataMap(data)) })
        pluginProxy.sendMessage("talk:get-preset", null, ResponseListener { sender, data -> onCharacterUpdate(MessageDataMap(data)) })

        /* Registering "Open chat" button in menu. */
        pluginProxy.sendMessage(SetEventLink(
                "gui:menu-action",
                "chat:open",
                pluginProxy.getString("chat.open")
        ))

        val registerPanel = SetPanel("chat", SetPanel.PanelType.WINDOW, SetPanel.ActionType.SET,
                Control(
                        Control.ControlType.CustomizableTextArea,
                        "textarea",
                        historyToChat(),
                        null,
                        null,
                        mapOf( "width" to 35.0, "height" to 22.0)
                ),
                InlineControls("input-line",
                    Control(
                            Control.ControlType.TextField,
                            mapOf(
                                    "id" to "input",
                                    "onChangeTag" to "chat:update-textfield",
                                    "enterTag" to "chat:user-said"
                            )
                    ),
                    Control(
                            Control.ControlType.Button,
                            mapOf(
                                    "value" to pluginProxy.getString("send"),
                                    "onChangeTag" to "chat:update-textfield",
                                    "enterTag" to "chat:user-said"
                            )
                    ),
                    Control(
                            Control.ControlType.Button,
                            mapOf(
                                    "value" to "⚙",
                                    "dstPanel" to pluginProxy.getId() + "-options"
                            )
                    ),
                    Control(
                            Control.ControlType.Button,
                            mapOf(
                                    "value" to "?",
                                    "msgTag" to "DeskChan:commands-list"
                            )
                     )
                )
        )
        registerPanel.name = pluginProxy.getString("chat")
        registerPanel.onClose = "chat:closed"

        pluginProxy.sendMessage(registerPanel)

        setupOptions()
        log("setup chat window completed")

        return true
    }

    fun onCharacterUpdate(map: MessageDataMap) {
        characterName = map.getString("name", "DeskChan")

        val tags = map.get("tags") as Map<String, *>?
        if (tags != null) {
            val un = map["usernames"] ?: return
            if (un is String) {
                userName = un
            } else {
                val list = un as Collection<*>
                if (list.isNotEmpty())
                    userName = list.iterator().next() as String
            }
        }
    }

    /** Chat drawing.  */
    fun setupChat() {
        if (!chatIsOpened) return

        pluginProxy.sendMessage(SetPanel("chat", SetPanel.PanelType.WINDOW, SetPanel.ActionType.UPDATE,
                Control(Control.ControlType.TextField, "input", ""),
                Control(Control.ControlType.CustomizableTextArea, "textarea", historyToChat())
        ))
    }

    /** Options window drawing.  */
    fun setupOptions() {
        val registerPanel = SetPanel("options", SetPanel.PanelType.SUBMENU, SetPanel.ActionType.SET,
                Control(
                        Control.ControlType.Spinner,
                        "length",
                        properties.getInteger("length"),
                        "label", pluginProxy.getString("log-length"),
                        "min", 1,
                        "max", 20000
                ),
                Control(
                        Control.ControlType.ColorPicker,
                        "user-color",
                        properties.getString("user-color"),
                        "label", pluginProxy.getString("user-color"),
                        "msgTag", "chat:set-user-color"
                ),
                Control(
                        Control.ControlType.ColorPicker,
                        "deskchan-color",
                        properties.getString("deskchan-color"),
                        "label", pluginProxy.getString("deskchan-color"),
                        "msgTag", "chat:set-deskchan-color"
                ),
                Control(
                        Control.ControlType.FontPicker,
                        "user-font",
                        properties.getString("user-font"),
                        "label", pluginProxy.getString("user-font")
                ),
                Control(
                        Control.ControlType.FontPicker,
                        "deskchan-font",
                        properties.getString("deskchan-font"),
                        "label", pluginProxy.getString("deskchan-font")
                ),
                Control(
                        Control.ControlType.Button,
                        "clear",
                        pluginProxy.getString("clear"),
                        "msgTag", "chat:clear"
                )
        )
        registerPanel.name = pluginProxy.getString("options")
        registerPanel.onSave = "chat:save-options"

    }

    internal fun saveOptions() {
        properties.save()
    }

    companion object {
        private lateinit var pluginProxy: PluginProxyInterface
        private lateinit var properties: PluginProperties

        internal fun log(text: String) {
            pluginProxy.log(text)
        }

        internal fun log(e: Throwable) {
            pluginProxy.log(e)
        }
    }
}
