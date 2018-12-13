package info.deskchan.weather

import info.deskchan.MessageData.Core.AddCommand
import info.deskchan.MessageData.Core.SetEventLink
import info.deskchan.MessageData.GUI.Control
import info.deskchan.MessageData.GUI.SetPanel
import info.deskchan.core.MessageDataMap
import info.deskchan.core.MessageListener
import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxyInterface
import java.text.SimpleDateFormat
import java.util.*

class Main : Plugin {

    private lateinit var server: WeatherServer

    override fun initialize(pluginProxyIn: PluginProxyInterface): Boolean {
        pluginProxy = pluginProxyIn
        instance = this

        val properties = pluginProxy.getProperties()
        properties.load()
        properties.putIfHasNot("city", "Nowhere")
        properties.putIfHasNot("scale-type", "celsius")

        pluginProxy.setConfigField("name", getString("plugin.name"))
        pluginProxy.setConfigField("short-description", getString("plugin.short-description"))
        pluginProxy.setConfigField("link", "https://forum.deskchan.info/topic/51/weather")


        server = YahooServer()

        try {
            properties.assert("weather-now-temp", "weather-now-time", "weather-now-state")
            val forecast = TimeForecast(
                    properties.getDateTimeFromStamp("weather-now-time", 0),
                    Temperature.fromString(properties.getString("weather-now-temp")!!),
                    properties.getString("weather-now-state")!!
            )
            server.loadFromProperties (forecast)
        } catch (e: Exception){}
        //server = OpenWeatherServer()

        pluginProxy.addMessageListener("weather:set-options", MessageListener { sender, tag, dat ->
            val data = MessageDataMap(dat)

            val city = data.getString("city")
            if (city == null || city.length < 2) {
                log(Exception(getString("info.no-city")))
            } else {
                pluginProxy.getProperties().put("city", city)
            }

            val scaleType = data.getString("scale-type")
            if (scaleType != null){
                pluginProxy.getProperties().put("scale-type", scaleType)
            }
            pluginProxy.getProperties().save()

            properties["city"] = city
            object : Thread() {
                override fun run() {
                    server.drop()
                    pluginProxy.getProperties().save()
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
            val calendar = data.getDateTimeFromStamp("date")
            if (calendar == null) {
                say["text"] = getString("now") + " " + server.getNow()
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
                val now = Calendar.getInstance()
                if (now.timeInMillis > calendar.timeInMillis && calendar.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
                    say["text"] = getString("error.past-date")
                } else if (Math.abs(now.timeInMillis - calendar.timeInMillis) < 3600000) {
                    say["text"] = getString("now") + " " + server.getNow()
                } else {
                    var days = 0
                    while (calendar.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                        days++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    }
                    if (days >= server.getDaysLimit()) {
                        say["text"] = getString("error.too-distant-date")
                    } else {
                        say["text"] = server.getByDay(days).toString()
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

    fun checkLocation() {
        val ret = server.checkLocation()
        updateOptionsTab(ret ?: getString("error"))
    }

    fun setupOptionsTab() {

        val message = SetPanel("options", SetPanel.PanelType.SUBMENU, SetPanel.ActionType.SET,
                Control(
                        Control.ControlType.ComboBox,
                        "scale-type",
                        scaleTypes.indexOf(pluginProxy.getProperties().getString("scale-type", "celsius")),
                        getString("scale-type.description"),
                        null,
                        mapOf<String, Any>("values" to scaleTypes)
                ),
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
        message.onSave = "weather:set-options"

        pluginProxy.sendMessage(message)
    }

    private fun updateOptionsTab(locationResult: String) {
        val message = SetPanel("options", SetPanel.PanelType.SUBMENU, SetPanel.ActionType.UPDATE,
                Control(
                        Control.ControlType.TextField,
                        "city",
                        locationResult
                )
        )
        pluginProxy.sendMessage(message)
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

        lateinit var pluginProxy: PluginProxyInterface

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

        private val timeFormatter = SimpleDateFormat("HH:mm")
        fun formatTime(time: Long) {
            val c = Calendar.getInstance()
            c.timeInMillis = time
            timeFormatter.format(c)
        }

        fun saveCurrentWeather(forecast: TimeForecast){
            pluginProxy.getProperties().put("weather-now-temp", forecast.temp)
            pluginProxy.getProperties().put("weather-now-time", forecast.time.timeInMillis)
            pluginProxy.getProperties().put("weather-now-state", forecast.weather)
            pluginProxy.getProperties().save()
        }

        private val scaleTypes = listOf("celsius", "fahrenheit")
    }
}
