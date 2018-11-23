package info.deskchan.notification_manager;

import info.deskchan.MessageData.Core.AddCommand
import info.deskchan.MessageData.Core.SetEventLink
import info.deskchan.MessageData.GUI.Control
import info.deskchan.MessageData.GUI.SetPanel
import info.deskchan.core.MessageDataMap
import info.deskchan.core.MessageListener
import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import java.text.SimpleDateFormat

import java.util.*;

public class Main : Plugin {

    companion object {
        private lateinit var pluginProxy: PluginProxyInterface
        private var phraseIdCounter = 0
    }

    /** Single phrase in notification. **/
    private class NotificationPhrase {
        public val text: String
        public val sender: String?
        public val date: Date
        public val id: Int

        constructor(text: String, sender: String?){
            this.text = text
            this.sender = sender
            date = Date()
            id = phraseIdCounter
            phraseIdCounter++
        }
        public fun getAsControl(): Control {
            var dateString = "(" + SimpleDateFormat("HH:mm:ss").format(date) + ") "
            val map = Control(
                    Control.ControlType.Button,
                    "notification" + id,
                    "X",
                    "label", dateString + (if (sender != null) "[" + sender + "]: " else "") + text,
                    "msgTag", "notification:delete",
                    "msgData", id
            )

            return map
        }
    }

    private var managerIsOpened = false
    private val history = mutableListOf<NotificationPhrase>()
    private val logLength = 12
    private var currentQuery = ""

    private lateinit var EMPTY: Control

    private fun historyToNotification(): MutableList<Map<String, Any>>{
        val ret = mutableListOf<Map<String, Any>>()
        val list = history.subList(Math.max(history.size - logLength, 0), history.size)
        if (list.isEmpty()){
            ret.add(EMPTY)
        } else {
            list.forEach { it -> ret.add(it.getAsControl()) }
        }

        return ret
    }

    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {

        Main.pluginProxy = pluginProxy
        pluginProxy.setConfigField("name", pluginProxy.getString("plugin.name"))

        EMPTY = Control(
                Control.ControlType.Label,
                null,
                pluginProxy.getString("no-notifications"),
                "width", 350
        )

        /* Open chat request.
        * Public message
        * Params: None
        * Returns: None */
        pluginProxy.addMessageListener("notification:open", MessageListener { sender, tag, data ->
            pluginProxy.sendMessage(SetPanel(
                    "notification",
                    SetPanel.PanelType.WINDOW,
                    SetPanel.ActionType.SHOW
            ))
            managerIsOpened = true;
        })

        pluginProxy.sendMessage(AddCommand(
                "notification:open",
                "notifications.open-info"
        ))

        pluginProxy.sendMessage(SetEventLink(
            "gui:keyboard-handle",
            "notification:open",
            "ALT+N"
        ))

        /* Chat has been closed through GUI. */
        pluginProxy.addMessageListener("notification:closed", MessageListener { sender, tag, data ->
            managerIsOpened = false;
        })

        /* Updated textfield input. */
        pluginProxy.addMessageListener("notification:update-textfield", MessageListener {sender, tag, data ->
            //currentQuery = data.toString();
        })

        /* Someone made request to clear all messages from notification window. We're clearing it. */
        pluginProxy.addMessageListener("notification:clear", MessageListener {sender, tag, data ->
            history.clear()
            setPanel()
        });

        pluginProxy.addMessageListener("notification:delete", MessageListener {sender, tag, data ->
            val id = data as Int
            history.removeIf { it -> it.id == id }
            setPanel()
        })


        /* New notification occurred */
        pluginProxy.addMessageListener("DeskChan:notify", MessageListener {sender, tag, data ->
            val map = MessageDataMap("message", data)
            if (map.containsKey("msgData"))
                map.put("message", map.getString("msgData"))

            if (!map.containsKey("message"))
                return@MessageListener

            history.add(NotificationPhrase(map.getString("message")!!, sender))
            setPanel()
        })

        /* Registering "Open Notification Manager" button in menu. */
        pluginProxy.sendMessage(SetEventLink(
            "gui:menu-action",
            "notification:open",
            pluginProxy.getString("notifications.open")
        ))

        setPanel()

        pluginProxy.log("Notification Manager loading complete")
        return true
    }

    fun setPanel(){
        val message = SetPanel(
                "notification",
                SetPanel.PanelType.WINDOW,
                SetPanel.ActionType.SET,
                pluginProxy.getString("window-name"),
                "notification:closed",
                null
        )
        val controls = historyToNotification()
        controls.add(Control(
                Control.ControlType.Button,
                "clear",
                pluginProxy.getString("clear"),
                "msgTag", "notification:clear"
        ))
        message.controls = controls
        Main.pluginProxy.sendMessage(message)
    }

}
