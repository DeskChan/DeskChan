package info.deskchan.weather

import java.util.*

class Temperature private constructor(private val value: Int) {

    fun asCelsius(): Int {
        return value
    }

    fun asFahrenheit(): Int {
        return (value / 0.555555).toInt() + 32
    }

    fun toCelsiusString(): String {
        return toString(asCelsius(), 'C')
    }

    fun toFahrenheitString(): String {
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
    }
}

class DayForecast(val day: Calendar, val tempLow: Temperature, val tempHigh: Temperature, val weather: String) {

    fun toCelsiusString(): String {
        return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) +
                ", " + day.get(Calendar.DAY_OF_MONTH) + ": " + tempHigh.toCelsiusString() + "/" + tempLow.toCelsiusString() + ", " + Main.getString(weather)
    }

    fun toFahrenheitString(): String {
        return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) +
                ", " + day.get(Calendar.DAY_OF_MONTH) + ": " + tempHigh.toFahrenheitString() + "/" + tempLow.toFahrenheitString() + ", " + Main.getString(weather)
    }
}

class TimeForecast(val temp: Temperature, val weather: String) {

    fun toCelsiusString(): String {
        return temp.toCelsiusString() + ", " + Main.getString(weather)
    }

    fun toFahrenheitString(): String {
        return temp.toFahrenheitString() + ", " + Main.getString(weather)
    }
}
