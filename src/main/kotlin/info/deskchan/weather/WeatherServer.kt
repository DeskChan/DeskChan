package info.deskchan.weather

interface WeatherServer {

    /** Get current weather.  */
    fun getNow(): TimeForecast?

    /** Get time of last update, as string.  */
    fun getLastUpdate(): String

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

}