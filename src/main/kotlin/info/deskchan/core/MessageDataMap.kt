package info.deskchan.core

import org.json.JSONArray
import org.json.JSONObject
import java.lang.RuntimeException
import java.util.*

open class MessageDataMap : HashMap<String, Any?>{

    constructor()

    /** Clone map **/
    constructor(data: Any?){
        if (data != null) putAll(data as Map<String, Any?>)
    }

    /** If data is Map - clones it, elsewhere creates new map and puts data on key specified. **/
    constructor(key: String, data: Any?) : super(){
        when (data){
            is Map<*,*> -> putAll(data as Map<String, Any?>)
            null -> return
            else -> put(key, data)
        }
    }

    constructor(vararg data: Any?){
        var i = 0
        while (i+1 < data.size) {
            put(data[i].toString(), data[i + 1])
            i += 2
        }
    }

    /** Get value converted to string if (value != null && !value.isEmpty()), else returns null. **/
    fun getString(key:String) : String? {
        val value = get(key)?.toString()
        if (value != null && !value.isEmpty())
            return value
        return null
    }

    /** Get value converted to string or default if no such key found. **/
    fun getString(key:String, default: String) = getString(key) ?: default

    /** Get value converted to long or null if no such key found or cannot convert it to long. **/
    fun getLong(key:String) : Long?{
        try {
            val obj:Any? = get(key)
            if (obj is Number)
                return obj.toLong()
            else if (obj == null)
                return null
            else
                return obj.toString().toDouble().toLong()
        } catch (e: Exception){ }
        return null
    }

    /** Get value converted to long or default if no such key found or cannot convert it to long. **/
    fun getLong(key: String, default: Number) : Long = getLong(key) ?: default.toLong()

    /** Get value converted to integer or null if no such key found or cannot convert it to long. **/
    fun getInteger(key: String) : Int? = getLong(key)?.toInt()

    /** Get value converted to integer or default if no such key found or cannot convert it to long. **/
    fun getInteger(key: String, default: Number) : Int = getLong(key)?.toInt() ?: default.toInt()

    fun getDouble(key:String) : Double?{
        try {
            val obj:Any? = get(key)
            if (obj is Number)
                return obj.toDouble()
            else if (obj == null)
                return null
            else
                return obj.toString().toDouble()
        } catch (e: Exception){ }
        return null
    }

    /** Get value converted to double or default if no such key found or cannot convert it to long. **/
    fun getDouble(key:String, default: Number) : Double = getDouble(key) ?: default.toDouble()

    /** Get value converted to double or null if no such key found or cannot convert it to long. **/
    fun getFloat(key: String) : Float? = getDouble(key)?.toFloat()

    /** Get value converted to float or default if no such key found or cannot convert it to long. **/
    fun getFloat(key: String, default: Number) : Float = getDouble(key)?.toFloat() ?: default.toFloat()

    /** Get value converted to boolean or null if no such key found. **/
    fun getBoolean(key:String) : Boolean?{
        try {
            val obj:Any? = get(key)
            if (obj is Boolean)
                return obj
            else if (obj == null)
                return null
            else {
                val bool:String = obj.toString().toLowerCase().trim()
                return bool == "true" || bool == "1"
            }
        } catch (e: Exception){  }
        return null
    }

    /** Get value converted to JSONArray. If value is absent or cannot be converted, returns JSONArray of value (or empty) if force = true, else null. **/
    fun getJSONArray(key: String, force: Boolean = true) : JSONArray? {
        if (!containsKey(key)) return if (force) JSONArray() else null

        try {
            var value = getString(key)!!
            if (!value.startsWith("["))
                value = "[" + value
            if (!value.endsWith("]"))
                value = value + "]"
            return JSONArray(value)
        } catch (e: Exception){ }

        return when (force){
            true -> {
                val ar = JSONArray()
                ar.put(get(key))
                ar
            }
            false -> null
        }
    }

    /** Get value converted to JSONObject. If value is absent or cannot be converted, returns JSONObject of {"value": value} (or empty) if force = true, else null. **/
    fun getJSONObject(key: String, force: Boolean = true) : JSONObject? {
        if (!containsKey(key)) return if (force) JSONObject() else null

        try {
            var value = getString(key)!!
            if (!value.startsWith("{"))
                value = "{" + value
            if (!value.endsWith("}"))
                value = value + "}"
            return JSONObject(value)
        } catch (e: Exception){ }

        return when (force){
            true -> {
                val ar = JSONObject()
                ar.put("value", get(key))
                ar
            }
            false -> null
        }
    }

    /** Get value converted to Collection<Map<String, Object>. If value is absent or cannot be converted, returns listOf(value as Map) (or empty) if force = true, else null. **/
    fun getListOfMap(key: String, force: Boolean = true) : Collection<Map<String, Any?>>? {
        if (!containsKey(key)) return if (force) mutableListOf() else null

        try {
            return get(key) as Collection<Map<String, Any?>>?
        } catch (e: Exception){
            try {
                return mutableListOf(get(key) as Map<String, Any?>)
            } catch (e2: Exception){ }
        }

        return when (force){
            true -> mutableListOf()
            false -> null
        }
    }

    /** Get value as Calendar, with suggestion that value is Long, UNIX timestamp in ms. Returns null otherwise. **/
    fun getDateTimeFromStamp(key: String) : Calendar? {
        val value = getLong(key)
        if (value == null) return null

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = value
        return calendar
    }

    /** Get value as Calendar, with suggestion that value is Long, UNIX timestamp in ms. Returns default otherwise. **/
    fun getDateTimeFromStamp(key: String, default: Long) : Calendar {
        val value = getLong(key)?: default

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = value
        return calendar
    }

    /** Get value as Calendar, with suggestion that value is Long, UNIX timestamp in ms. Returns default otherwise. **/
    fun getDateTimeFromStamp(key: String, default: Calendar) : Calendar = getDateTimeFromStamp(key)?: Calendar.getInstance()

    /** Get value converted to file or null if no such key found. **/
    fun getFile(key: String) : Path? {
        if (get(key) == null) return null
        try {
            return Path(get(key).toString())
        } catch (e: Exception){ }
        return null
    }

    /** Get value converted to file or default if no such key found. **/
    fun getFile(key: String, default: String) : Path = getFile(key) ?: Path(default)

    /** Get one of values given if (value.toString().toLowerCase() == it.toString().toLowerCase()), else returns null **/
    fun<T: Any> getOneOf(key: String, values: Iterable<T>) : T? {
        var v = get(key)
        if (v == null) return null
        v = v.toString().toLowerCase()
        values.forEach { if (it.toString().toLowerCase() == v) return it }
        return null
    }

    /** Get one of values given if (value.toString().toLowerCase() == it.toString().toLowerCase()), else returns null **/
    fun<T: Any> getOneOf(key: String, values: Array<T>) : T? = getOneOf(key, values.toList())

    /** Get one of values given if (value.toString().toLowerCase() == it.toString().toLowerCase()), else returns null **/
    fun<T: Any> getOneOf(key: String, values: Collection<T>, default: T) : T = getOneOf(key, values) ?: default

    /** Get one of values given if (value.toString().toLowerCase() == it.toString().toLowerCase()), else returns null **/
    fun<T: Any> getOneOf(key: String, values: Array<T>, default: T) : T = getOneOf(key, values) ?: default

    /** Get value converted to boolean or default if no such key found . **/
    fun getBoolean(key:String, default:Boolean) : Boolean = getBoolean(key) ?: default

    override fun put(key: String, value: Any?) =
        when (value){
            "", "null", null -> super.remove(key)
            else -> super.put(key, value)
        }

    override fun putAll(m: Map<out String, *>) {
        (m as Map<*,*>).forEach{ k, v -> put(k.toString(), v)}
    }

    /** Put value if there is no such key in properties and value is not null. **/
    fun putIfHasNot(key: String, value:Any?){
        if (value != null && get(key) == null) put(key, value)
    }

    /** Put value if value is not null. **/
    fun putIfNotNull(key: String, value:Any?){
        if (value != null) put(key, value)
    }

    /** Checks if all keys given are present. **/
    @Throws(MessageDataDeserializationException::class)
    fun assert(vararg checkKeys: String){
        var notFound = mutableListOf<String>()
        checkKeys.forEach { if (it !in keys) notFound.add(it) }

        if (notFound.isNotEmpty()){
            throw MessageDataDeserializationException("Given data is not enough. Needed keys: $notFound")
        }
    }

    /** Checks if all keys given are present. **/
    @Throws(MessageDataDeserializationException::class)
    fun assertForTag(sender: String, receiver: String, tag: String, vararg checkKeys: String){
        var notFound = mutableListOf<String>()
        checkKeys.forEach { if (it !in keys) notFound.add(it) }

        if (notFound.isNotEmpty()){
            throw MessageDataDeserializationException("Given data for tag \"$tag\" is not enough. Received from $sender to $receiver. Needed keys: $notFound")
        }
    }

    /** Checks if any key given is present. **/
    @Throws(MessageDataDeserializationException::class)
    fun assertAny(vararg checkKeys: String){
        checkKeys.forEach { if (it in keys) return; }
        throw MessageDataDeserializationException("Given data is not enough. Needed any of this keys: ${checkKeys.toList()}")
    }

    /** Checks if all keys given are present. **/
    @Throws(MessageDataDeserializationException::class)
    fun assertAnyForTag(sender: String, receiver: String, tag: String, vararg checkKeys: String){
        checkKeys.forEach { if (it in keys) return; }
        throw MessageDataDeserializationException("Given data for tag \"$tag\" is not enough. Received from $sender to $receiver. Needed any of this keys: ${checkKeys.toList()}")
    }
}
class MessageDataDeserializationException : RuntimeException {
    constructor(text: String) : super(text)
}