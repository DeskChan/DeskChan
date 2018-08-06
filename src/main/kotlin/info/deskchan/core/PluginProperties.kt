package info.deskchan.core

import java.io.IOException
import java.nio.file.Files
import java.util.*

class PluginProperties(private val proxyInterface: PluginProxyInterface) : MessageDataMap() {

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