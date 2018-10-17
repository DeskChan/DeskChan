package info.deskchan.weather

import info.deskchan.MessageData.Core.AddCommand
import info.deskchan.MessageData.Core.SetEventLink
import info.deskchan.MessageData.GUI.Control
import info.deskchan.MessageData.GUI.SetPanel
import info.deskchan.core.MessageDataMap
import info.deskchan.core.MessageListener
import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxyInterface
import java.util.*

class Main : Plugin {

    private lateinit var server: WeatherServer

    override fun initialize(pluginProxyIn: PluginProxyInterface): Boolean {
        pluginProxy = pluginProxyIn
        instance = this

        val properties = pluginProxy.getProperties()
        properties.load()
        properties.putIfHasNot("city", "Nowhere")

        pluginProxy.setConfigField("name", getString("plugin.name"))
        pluginProxy.setConfigField("short-description", getString("plugin.short-description"))
        pluginProxy.setConfigField("link", "https://forum.deskchan.info/topic/51/weather")
        pluginProxy.setConfigField("temp-type", "celsius")

        server = YahooServer()
        //server = OpenWeatherServer()

        pluginProxy.addMessageListener("weather:set-city", MessageListener { sender, tag, data ->
            val city = MessageDataMap("city", data).getString("city")
            if (city == null || city.length < 2) {
                log(Exception(getString("info.no-city")))
                return@MessageListener
            }

            properties["city"] = city
            object : Thread() {
                override fun run() {
                    server.drop()
                    checkLocation()
                    saveOptions()
                }
            }.start()
        })

        pluginProxy.sendMessage(AddCommand("weather:say-weather", getString("command.info")))
        pluginProxy.sendMessage(SetEventLink("speech:get", "weather:say-weather", getString("say-weather.command")))

        pluginProxy.addMessageListener("weather:say-weather", MessageListener { sender, tag, dat ->
            val say = mutableMapOf<String, Any>()
            say["priority"] = 2000
            say["skippable"] = false

            val forecast = server.getNow()
            if (forecast == null) {
                say["intent"] = "NO_NETWORK"
                pluginProxy.sendMessage("DeskChan:request-say", say)
                return@MessageListener
            }

            val data = MessageDataMap()
            val date = data.getLong("date")
            if (date == null) {
                say["text"] = getString("now") + " " + server.getNow().toString() + ", " + Main.getString("lastUpdate") + ": " + server.getLastUpdate()
            } /*else if (value.equals("all")) {
                List<String> b = new ArrayList<>(11);
                b.add(getString("now") + " - " + server.getNow().toString());
                for (int i = 0; i < 10; i++) {
                    b.add(server.getByDay(i).toString());
                }
                b.add(Main.getString("lastUpdate")+": "+server.getLastUpdate());
                say.put("text", b);
            } */
            else {
                val calendar = Calendar.getInstance()
                val now = Calendar.getInstance()
                calendar.timeInMillis = date
                if (now.timeInMillis > calendar.timeInMillis && calendar.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
                    say["text"] = "Я пока не могу сказать, какая погода была в прошлом. И вообще, надо смотреть в будущее."
                } else if (Math.abs(now.timeInMillis - calendar.timeInMillis) < 3600000) {
                    say["text"] = getString("now") + " " + server.getNow().toString() + ", " + Main.getString("lastUpdate") + ": " + server.getLastUpdate()
                } else {
                    var days = 0
                    while (calendar.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                        days++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    }
                    if (days >= server.getDaysLimit()) {
                        say["text"] = "Ой, прости, этот день будет слишком нескоро. Я не знаю, какая будет погода."
                    } else {
                        say["text"] = server.getByDay(days).toString() + ", " + Main.getString("lastUpdate") + ": " + server.getLastUpdate()
                    }
                }
            }
            pluginProxy.sendMessage("DeskChan:say", say)
        })

        /*pluginProxy.addMessageListener("talk:reject-quote", MessageListener { sender, tag, data ->
            val list = data as List<Map<String, Any>>
            val quotes_list = ArrayList<Map<String, Any>>()
            if (list != null) {
                val now = server!!.now
                if (now != null) {
                    for (entry in list) {
                        val types = entry["weather"] as Collection<*> ?: continue
                        for (type in types) {
                            if (!isWeatherMatch(type, now.weather, now.temp)) {
                                quotes_list.add(entry)
                                break
                            }
                        }
                    }
                }
            }
            pluginProxy.sendMessage(sender, quotes_list)
        })*/

        setupOptionsTab()
        object : Thread() {
            override fun run() {
                checkLocation()
                server.getNow()
            }
        }.start()
        log("Initialization completed")
        return true
    }

    internal fun checkLocation() {
        val ret = server.checkLocation()
        updateOptionsTab(ret ?: getString("error"))
    }

    internal fun setupOptionsTab() {
        val message = SetPanel("options", SetPanel.PanelType.SUBMENU, SetPanel.ActionType.SET,
                Control(
                        Control.ControlType.TextField,
                        "city",
                        null,
                        getString("city")
                ),
                Control(
                        Control.ControlType.Label,
                        null,
                        getString("info.check")
                )
        )
        message.name = getString("options")
        message.onSave = "weather:set-city"

        pluginProxy.sendMessage(message)
    }

    internal fun updateOptionsTab(locationResult: String) {
        val message = SetPanel("options", SetPanel.PanelType.SUBMENU, SetPanel.ActionType.UPDATE,
                Control(
                        Control.ControlType.TextField,
                        "city",
                        locationResult
                )
        )
        pluginProxy.sendMessage(message)
    }

    internal fun saveOptions() {
        pluginProxy.getProperties().save()
    }

    override fun unload() {}

    fun isWeatherMatch(match: String, current: String, temp: Temperature): Boolean {
        if (match == "cold")
            return temp.asCelsius() < 5
        if (match == "hot")
            return temp.asCelsius() > 27
        if (match == "good")
            return current == "clear" || current == "cloudy"
        return if (match == "bad") current != "clear" && current != "cloudy" else current == match
    }

    companion object {

        private lateinit var pluginProxy: PluginProxyInterface

        val city: String?
            get() = pluginProxy.getProperties().getString("city")

        private lateinit var instance: Main

        internal fun log(text: String) {
            pluginProxy.log(text)
        }

        internal fun log(e: Throwable) {
            pluginProxy.log(e)
        }

        fun getString(text: String): String {
            return pluginProxy.getString(text)
        }
    }
}
