package info.deskchan.weather

import java.text.SimpleDateFormat
import java.util.*

abstract class TemperatureString {

    abstract fun toCelsiusString(): String

    abstract fun toFahrenheitString(): String

    override fun toString(): String{
        if (Main.pluginProxy.getProperties().getString("scale-type", "").toLowerCase() == "fahrenheit")
            return toFahrenheitString()
        return toCelsiusString()
    }
}
class Temperature private constructor(private val value: Int) : TemperatureString() {

    fun asCelsius(): Int {
        return value
    }

    fun asFahrenheit(): Int {
        return (value / 0.555555).toInt() + 32
    }

    override fun toCelsiusString(): String {
        return toString(asCelsius(), 'C')
    }

    override fun toFahrenheitString(): String {
        return toString(asFahrenheit(), 'F')
    }

    private fun toString(value: Int, tempSymbol: Char): String {
        return if (value == 0) "0 °$tempSymbol" else (if (value > 0) "+" else "-") + value + " °" + tempSymbol
    }

    companion object {

        fun fromCelsius(value: Int): Temperature {
            return Temperature(value)
        }

        fun fromFahrenheit(value: Int): Temperature {
            return Temperature((0.555555 * (value - 32)).toInt())
        }

        fun fromString(str: String): Temperature {
            val sb = StringBuilder()
            for (i in 0..str.length-1){
                if (str[i].isDigit() || str[i] == '+' || str[i] == '-')
                    sb.append(str[i])
                else
                    break
            }
            if (str.endsWith("F"))
                return fromFahrenheit(sb.toString().toInt())
            return fromCelsius(sb.toString().toInt())
        }
    }
}

class DayForecast(val day: Calendar, val tempLow: Temperature, val tempHigh: Temperature, val weather: String) : TemperatureString() {

    override fun toCelsiusString(): String {
        return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) +
                ", " + day.get(Calendar.DAY_OF_MONTH) + ": " + tempHigh.toCelsiusString() + "/" + tempLow.toCelsiusString() + ", " + Main.getString(weather)
    }

    override fun toFahrenheitString(): String {
        return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) +
                ", " + day.get(Calendar.DAY_OF_MONTH) + ": " + tempHigh.toFahrenheitString() + "/" + tempLow.toFahrenheitString() + ", " + Main.getString(weather)
    }

}

class TimeForecast(val time: Calendar, val temp: Temperature, val weather: String) : TemperatureString() {

    private fun getTimeString() =  Main.getString("temp-for-time") + ": " + SimpleDateFormat("HH:mm").format(time.time)

    override fun toCelsiusString(): String {
        return getTimeString() + ", " + temp.toCelsiusString() + ", " + Main.getString(weather)
    }

    override fun toFahrenheitString(): String {
        return getTimeString() + ", " + temp.toFahrenheitString() + ", " + Main.getString(weather)
    }

}


