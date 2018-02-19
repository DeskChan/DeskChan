package info.deskchan.core

import java.io.IOException
import java.nio.file.Files
import java.util.*

class PluginProperties(private val proxyInterface: PluginProxyInterface) : HashMap<String, Any>(){

    /** Get value converted to string or null if no such key found. **/
    fun getString(key:String) : String? = get(key)?.toString()

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
    fun getLong(key: String, default: Long) : Long = getLong(key) ?: default

    /** Get value converted to integer or null if no such key found or cannot convert it to long. **/
    fun getInteger(key: String) : Int? = getLong(key)?.toInt()

    /** Get value converted to integer or default if no such key found or cannot convert it to long. **/
    fun getInteger(key: String, default: Int) : Int = getLong(key)?.toInt() ?: default

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
    fun getDouble(key:String, default:Double) : Double = getDouble(key) ?: default

    /** Get value converted to double or null if no such key found or cannot convert it to long. **/
    fun getFloat(key: String) : Float? = getDouble(key)?.toFloat()

    /** Get value converted to float or default if no such key found or cannot convert it to long. **/
    fun getFloat(key: String, default: Float) : Float = getDouble(key)?.toFloat() ?: default

    /** Get value converted to boolean or null if no such key found. **/
    fun getBoolean(key:String) : Boolean?{
        try {
            val obj:Any? = get(key)
            if (obj is Boolean)
                return obj
            else if (obj == null)
                return null
            else {
                val bool:String = obj.toString().toLowerCase()
                return bool == "true" || bool == "1"
            }
        } catch (e: Exception){  }
        return null
    }

    /** Get value converted to boolean or default if no such key found . **/
    fun getBoolean(key:String, default:Boolean) : Boolean = getBoolean(key) ?: default

    /** Put value if there is no such key in properties and value is not null. **/
    fun putIfHasNot(key: String, value:Any?){
        if (value != null && get(key) == null) put(key, value)
    }

    /** Put value if value is not null. **/
    fun putIfNotNull(key: String, value:Any?){
        if (value != null) put(key, value)
    }

    /** Loads properties from default location and overwrites current properties map. **/
    fun load(){
        load_impl(true)
    }

    /** Loads properties from default location and merges current properties map. **/
    fun merge(){
        load_impl(false)
    }

    private fun load_impl(clear: Boolean){
        val configPath = proxyInterface.dataDirPath.resolve("config.properties")
        val properties = Properties()
        try {
            val ip = Files.newInputStream(configPath)
            properties.load(ip)
            ip.close()
        } catch (e: Exception) {
            return
        }

        if (clear){
            for (key in keys)
                if (key !in properties.keys)
                    remove(key)
        }
        for ((key, value) in properties){
            val obj:Any? = get(key)
            try {
                if (obj is Number)
                    put(key.toString(), value.toString().toDouble())
                else if (obj is Boolean)
                    put(key.toString(), value.toString().toLowerCase().equals("true"))
                else
                    put(key.toString(), value.toString())
            } catch (e: Exception){
                put(key.toString(), value.toString())
            }
        }
        proxyInterface.log("Properties loaded")
    }

    /** Saves properties to default location. **/
    fun save(){
        if (size == 0) return
        val configPath = proxyInterface.dataDirPath.resolve("config.properties")
        try {
            val properties = Properties()
            for ((key, value) in this)
                properties.put(key, value.toString())

            val ip = Files.newOutputStream(configPath)
            properties.store(ip, proxyInterface.getId() + " config")
            ip.close()
        } catch (e: Exception) {
            proxyInterface.log(IOException("Cannot save file: " + configPath, e))
        }
    }
}