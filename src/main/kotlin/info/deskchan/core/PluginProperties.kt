package info.deskchan.core

import java.nio.file.Files
import java.util.*

class PluginProperties(private val proxyInterface: PluginProxyInterface) : HashMap<String, Any>(){

    fun getString(key:String) : String? = get(key)?.toString()

    fun getString(key:String, default: String) = getString(key) ?: default

    fun getLong(key:String) : Long?{
        try {
            val obj:Any? = get(key)
            if (obj is Number)
                return obj.toLong()
            else
                return obj.toString().toLong()
        } catch (e: Exception){ }
        return null
    }

    fun getLong(key: String, default: Long) : Long = getInteger(key)?.toLong() ?: default

    fun getInteger(key: String) : Int? = getLong(key)?.toInt()

    fun getInteger(key: String, default: Int) = getLong(key) ?: default

    fun getDouble(key:String) : Double?{
        try {
            val obj:Any? = get(key)
            if (obj is Number)
                return obj.toDouble()
            else
                return obj.toString().toDouble()
        } catch (e: Exception){ }
        return null
    }

    fun getDouble(key:String, default:Double) : Double = getDouble(key) ?: default

    fun getFloat(key: String) : Float? = getDouble(key)?.toFloat()

    fun getFloat(key: String, default: Float) : Float = getDouble(key)?.toFloat() ?: default

    fun getBoolean(key:String) : Boolean?{
        try {
            val obj:Any? = get(key)
            if (obj is Boolean)
                return obj
            else
                return obj.toString().toLowerCase().equals("true")
        } catch (e: Exception){ }
        return null
    }

    fun getBoolean(key:String, default:Boolean) : Boolean = getBoolean(key) ?: default

    fun putIfNull(key: String, value:Any?){
        if (value != null && get(key) == null) put(key, value)
    }

    fun load(){
        load_impl(true)
    }

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
    }

    fun save(){
        val configPath = proxyInterface.dataDirPath.resolve("config.properties")
        try {
            val properties = Properties()
            for ((key, value) in this)
                properties.put(key, value.toString())

            val ip = Files.newOutputStream(configPath)
            properties.store(ip, proxyInterface.getId() + " config")
            ip.close()
        } catch (e: Exception) {
            proxyInterface.log("Cannot save file: " + configPath)
        }
    }
}