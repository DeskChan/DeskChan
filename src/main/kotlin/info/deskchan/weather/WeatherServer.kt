package info.deskchan.weather

import java.util.*

interface WeatherServer {

    /** Get current weather.  */
    fun getNow(): TimeForecast?

    /** Get time of last update, as string. If null, not update was performed yet. */
    fun getLastUpdate(): Calendar?

    /** Get count of days for which weather is present.  */
    fun getDaysLimit(): Int

    /** Update weather from server.  */
    fun update()

    /** Get weather by day. day is an offset starting from today.  */
    fun getByDay(day: Int): DayForecast?

    /** Check is weather server can retrieve informtation for such location.  */
    fun checkLocation(): String?

    /** Drop weather information.  */
    fun drop()

    /** Set current weather from properties
     * Needed to lower requests to weather server.
     */
    fun loadFromProperties(now: TimeForecast)

}