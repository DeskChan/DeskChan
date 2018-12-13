package info.deskchan.MessageData.CoreEvents

import info.deskchan.core.LoggerLevel
import info.deskchan.core.MessageData

/**
 * Called every time some plugin wants to log text to console
 *
 * @property message Message to log
 * @property level Log level
 */
@MessageData.Tag("core-events:log")
class Log(val message: String) : MessageData {

    private var level: Int? = null

    /** Set log level **/
    fun setLevel(value: LoggerLevel){
        level = value.value
    }

    /** Get log level **/
    fun getLevel() = LoggerLevel.values().find { it.value == level }

}