package info.deskchan.external_loader.wrappers

import org.jetbrains.annotations.Nullable
import org.json.JSONArray
import org.json.JSONObject

abstract class MessageWrapper {

    val messageArgsCount = mapOf(
        "sendMessage" to 2,
        "callNextAlternative" to 4,
        "addMessageListener" to 2,
        "removeMessageListener" to 2,
        "setTimer" to 3,
        "cancelTimer" to 1,
        "setAlternative" to 3,
        "deleteAlternative" to 2,
        "getProperty" to 1,
        "setProperty" to 2,
        "getConfigField" to 1,
        "setConfigField" to 2,
        "getString" to 1,
        "log" to 1,
        "error" to 1,
        "initializationCompleted" to 0
    )

    class Message(
            val type: String,
            val requiredArguments: MutableList<Any?> = mutableListOf(),
            val additionalArguments: MutableMap<String, Any?> = mutableMapOf()
    ){

        fun getRequiredAsInt(index: Int) = (requiredArguments[index] as Number).toInt()

        fun getRequiredAsLong(index: Int) = (requiredArguments[index] as Number).toLong()

        fun getRequiredAsString(index: Int) = requiredArguments[index].toString()

    }

    abstract fun wrap(message: Message): Any

    abstract fun unwrap(text: String): Message

    fun serialize(data: Any?): Any {
        return when(data)
        {
            is Collection<*> -> dataToJSON(data)
            is Map<*, *> -> dataToJSON(data)
            is JSONArray -> dataToJSON(data)
            is JSONObject -> dataToJSON(data)
            is MessageWrapper.Message -> wrap(data)
            is Nullable -> "null"
            else -> data.toString().replace("\n", "\t")
        }
    }

    fun deserialize(data:Any?):Any? {
        if (data == null) return null
        if (data is List<*> || data is Map<*,*>) return data

        if (data is JSONArray) return JSONToData(data)
        if (data is JSONObject) return JSONToData(data)

        val rdata = data.toString().replace("\t", "\n")
        try {
            return JSONToData(JSONArray(rdata))
        } catch (e:Exception){ }
        try {
            return JSONToData(JSONObject(rdata))
        } catch (e:Exception){ }
        try {
            return rdata.toDouble()
        } catch (e:Exception){ }
        try {
            return rdata.toLong()
        } catch (e:Exception){ }
        try {
            if (rdata.toLowerCase() == "true")  return true
            if (rdata.toLowerCase() == "false") return false
        } catch (e:Exception){ }
        return rdata
    }

    fun dataToJSON(data: Map<*,*>) : JSONObject {
        val obj = JSONObject()
        data.entries.forEach { obj.put(it.key.toString().replace("\n", "\t"), serialize(it.value)) }
        return obj
    }

    fun dataToJSON(data: Collection<*>) : JSONArray {
        val obj = JSONArray()
        data.forEach { obj.put(serialize(it)) }
        return obj
    }

    fun dataToJSON(data: JSONObject) : JSONObject = dataToJSON(data.toMap())

    fun dataToJSON(data: JSONArray) : JSONArray = dataToJSON(data.toList())

    fun JSONToData(data: JSONObject):Any? {
        val obj = mutableMapOf<String, Any?>()
        data.toMap().forEach { obj.put(it.key.toString().replace("\t", "\n"), deserialize(it.value)) }
        return obj
    }

    fun JSONToData(data: JSONArray):Any? {
        val obj = mutableListOf<Any?>()
        data.forEach { obj.add(deserialize(it)) }
        return obj
    }

}
