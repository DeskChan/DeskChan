package info.deskchan.weather

import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class YahooServer : WeatherServer {
    protected var state = ConnectionState.NO

    private var now: TimeForecast? = null
    private var forecasts = arrayOfNulls<DayForecast>(DAYS_LIMIT)

    private var lastUpdate: Date? = null

    private val query: JSONObject
        @Throws(Exception::class)
        get() {
            val query: String

            query = URLEncoder.encode(Main.city, "UTF-8")

            var url = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22"
            url += "$query%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys"

            val out = StringBuilder()

            val DATA_URL = URL(url)
            val stream = DATA_URL.openStream()

            val buffer = CharArray(1024)

            val `in` = InputStreamReader(stream, "UTF-8")
            while (true) {
                val rsz = `in`.read(buffer, 0, buffer.size)
                if (rsz < 0)
                    break
                out.append(buffer, 0, rsz)
            }
            stream.close()

            return JSONObject(out.toString())
        }

    override fun getDaysLimit(): Int {
        return DAYS_LIMIT
    }

    protected enum class ConnectionState {
        NO, CONNECTING, ERROR
    }

    override fun getByDay(day: Int): DayForecast? {
        update()
        return forecasts[day]
    }

    override fun getNow(): TimeForecast? {
        update()
        return now
    }

    override fun checkLocation(): String? {
        try {
            state = ConnectionState.CONNECTING
            var json = query
            state = ConnectionState.NO
            json = json.getJSONObject("query")
            if (!json.has("results") || json.get("results") !is JSONObject) {
                Main.log(Exception(Main.getString("incorrect")))
                return null
            }

            json = json.getJSONObject("results")
            json = json.getJSONObject("channel")
            json = json.getJSONObject("location")
            return json.getString("city") + ", " + json.getString("country")
        } catch (e: Exception) {
            Main.log(Main.getString("info.lost-server"))
            state = ConnectionState.ERROR
            return null
        }

    }

    override fun getLastUpdate(): String {
        val format = SimpleDateFormat("HH:mm:ss")
        return format.format(lastUpdate)
    }

    override fun drop() {
        lastUpdate = null
    }

    override fun update() {
        object : Thread() {
            override fun run() {
                updateImpl()
            }
        }.start()
    }

    private fun updateImpl() {
        if (state != ConnectionState.NO || lastUpdate != null && (Date().time - lastUpdate!!.time) / 60000 < 30) return
        state = ConnectionState.CONNECTING

        Main.log("Trying to connect to weather server")
        now = null
        var i: Int
        i = 0
        while (i < DAYS_LIMIT) {
            forecasts[i] = null
            i++
        }

        var json: JSONObject
        try {
            json = query
            state = ConnectionState.NO
        } catch (e: Exception) {
            Main.log("Error while retrieving data from server")
            state = ConnectionState.ERROR
            return
        }

        try {
            json = json.getJSONObject("query")
            json = json.getJSONObject("results")
            json = json.getJSONObject("channel")
            json = json.getJSONObject("item")
            val cal = Calendar.getInstance()
            cal.time = Date()
            val array = json.getJSONArray("forecast")
            i = 0
            while (i < DAYS_LIMIT && i < array.length()) {
                val dayForecast = array.getJSONObject(i)
                forecasts[i] = DayForecast(cal.clone() as Calendar,
                        Temperature.fromFahrenheit(dayForecast.getInt("high")),
                        Temperature.fromFahrenheit(dayForecast.getInt("low")),
                        getWeatherString(dayForecast.getInt("code"))
                )
                cal.add(Calendar.DATE, 1)
                i++
            }
            json = json.getJSONObject("condition")
            now = TimeForecast(Temperature.fromFahrenheit(json.getInt("temp")), getWeatherString(json.getInt("code")))
            lastUpdate = Date()

        } catch (e: Exception) {
            state = ConnectionState.ERROR
            Main.log(Exception("Error while parsing data from server", e))
        }

    }

    private fun getWeatherString(code: Int): String {
        var text = "unknown"
        when (code) {
            0, 1, 2 -> text = "storm"
            3, 4, 37, 38, 39, 45, 47 -> text = "thunderstorm"
            5, 6, 7, 16, 18, 46 -> text = "snow"
            8, 9, 10, 40 -> text = "rain"
            11, 12 -> text = "heavy-rain"
            13, 14, 15, 41, 42, 43 -> text = "heavy-snow"
            17, 35 -> text = "hail"
            19, 20, 21, 22 -> text = "fog"
            23, 24, 25 -> text = "wind"
            26, 27, 28, 29, 30, 44 -> text = "cloudy"
            31, 32, 33, 34, 36 -> text = "clear"
        }
        return text
    }

    companion object {

        private val DAYS_LIMIT = 10
    }
}
