package info.deskchan.core

import org.apache.commons.io.IOUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

enum class Platform { All, Desktop, Mobile, Windows, Linux, Mac, Android, iOs }
val DEFAULT_PLATFORM = Platform.Desktop   // Sweet Dreams: change to ALL before the final release version

private const val TEMPLATE_INVALID_PROPERTY = "Manifest of plugin \"%s\" has invalid property: %s"

class PluginConfig {

    val data: MutableMap<String, Any> = mutableMapOf<String, Any>()

    constructor()

    constructor(type: String){
        data["type"] = type
    }

    constructor(map: Map<String, Any?>?) {
        if(map == null) return
        append(map)
    }

    fun append(map: Map<String, Any?>){
        map.forEach { t, u ->
            if(u!=null && u!="")
                data[t.toLowerCase()]=u
        }
    }

    fun append(key: String, value: Any){
        data[key.toLowerCase()] = value
    }
    fun appendFromJson(path: Path){
        if (!Files.isReadable(path) || Files.isDirectory(path)) {
            return
        }

        var manifestStr: String = ""
        try {
            Files.newInputStream(path).use { manifestInputStream ->
                manifestStr = IOUtils.toString(manifestInputStream, "UTF-8")
            }
        } catch (e: IOException) {
            PluginManager.log("Couldn't read file: $path")
            PluginManager.log(e)
        }

        val json: JSONObject
        try {
            json = JSONObject(manifestStr)
        } catch (e: JSONException) {
            PluginManager.log("Invalid manifest file: $path")
            PluginManager.log(e)
            return
        }
        json.toMap().forEach { t, u ->
            if(u!=null && u!=""){
                if(u is JSONArray){
                    data[t.toLowerCase()] = u.toList()
                } else if(u is JSONObject){
                    data[t.toLowerCase()] = u.toMap()
                }
            }
        }
    }

    val dependencyNames = listOf("deps" , "dep" , "dependencies" , "dependency")
    fun getDependencies() : List<String> = getList(dependencyNames)

    val extensionNames = listOf("extensions")
    fun getExtensions() : List<String> = getList(extensionNames)

    fun getList(possibleNames: List<String>) : List<String>{
        var value: Any? = null
        for(name in possibleNames){
            value = data[name]
            if(value==null) continue

            if(name != possibleNames[0]) {
                data.remove(name)
                data[possibleNames[0]] = value
            }
        }

        if(value == null){
            value = emptyList<String>()
            data[possibleNames[0]] = value
            return value
        } else if(value is List<*>){
            return value as List<String>
        } else {
            value = mutableListOf<String>(value.toString())
            data[possibleNames[0]] = value
            return value
        }
    }

    fun getType() : String = PluginProxy.getString(data.get("type")?.toString() ?: "unknown")

    fun get(key:String) : Any? = data[key]

    fun getShortDescription() : String {
        val sb = StringBuilder()
        sb.append(PluginProxy.getString("plugin-type")+": "+getType()+"\n")
        if(data.containsKey("authors"))
            sb.append(PluginProxy.getString("authors")+": "+get("authors")+"\n")
        if(data.containsKey("version"))
            sb.append(PluginProxy.getString("version")+": "+get("version")+"\n")
        if(data.containsKey("short-description"))
            sb.append(PluginProxy.getString("description")+": "+get("short-description")+"\n")

        return sb.toString().dropLast(1)
    }

    fun print(){
        println(data)
    }

    fun clone(): PluginConfig = PluginConfig(data)

    companion object{
        val Internal: PluginConfig
        init {
            Internal = PluginConfig()
            try {
                Internal.append("type", "Internal")
                Internal.append("version", CoreInfo.get("VERSION"))
                Internal.append("authors", CoreInfo.get("AUTHORS"))
            } catch (e: Exception){
                PluginManager.log(e)
            }
        }
    }
        /*private fun parse(manifest: JSONObject) {
            listOf("name", "version", "keywords", "license").forEach {
                if (manifest.has(it)) {
                    try {
                        data[it] = manifest.getString(it)
                    } catch (e: JSONException) {
                        PluginManager.log(TEMPLATE_INVALID_PROPERTY.format(name, it))
                        PluginManager.log(e)
                    }
                }
            }
            if (manifest.has("description")) {
                data["description"] = try {
                    manifest.getString("description")
                } catch (e: JSONException) {
                    val strings = manifest.getJSONObject("description")
                            .toMap()
                            .map { Pair(it.key.toString(), it.value.toString()) }
                            .toMap()
                    getLocalString(strings)
                } catch (e: JSONException) {
                    PluginManager.log(TEMPLATE_INVALID_PROPERTY.format(name, "description"))
                    null
                }
            }
            if (manifest.has("authors")) {
                try {
                    val authors = manifest.getString("authors").split(';')
                    data["authors"] = authors.map { AuthorParser.parse(it) }
                } catch (e: JSONException) {
                    try {
                        val authors = manifest.getJSONArray("authors")
                        data["authors"] = authors
                                .mapIndexed { i, _ -> authors.getJSONObject(i) }
                                .filter { it.has("name") }
                                .map {
                                    val email = if (it.has("email")) it.getString("email") else null
                                    val website = if (it.has("website")) it.getString("website") else null
                                    Author(
                                            it.getString("name"),
                                            email,
                                            website
                                    )
                                }
                    } catch (e: JSONException) {
                        PluginManager.log(TEMPLATE_INVALID_PROPERTY.format(name, "authors"))
                        PluginManager.log(e)
                    }
                }
            }

        }

        fun parseJsonManifest(name: String, path: Path): Manifest {
            val json = readManifestJsonFile(path) ?: return Manifest(name)
            return parseManifest(name, json)
        }

        fun parseJsonPluginManifest(id: String, path: Path): PluginConfig {
            val json = readManifestJsonFile(path) ?: return PluginConfig(id)

            val dependencies = mutableListOf<String>()
            listOf("deps", "dependencies").forEach {
                if (json.has(it)) {
                    try {
                        val deps = json.getJSONArray(it)
                        dependencies.addAll(deps.filter { it is String }.map { it.toString() })
                    } catch (e: JSONException) {
                        log(TEMPLATE_INVALID_PROPERTY.format(id, it))
                        log(e)
                    }
                }
            }

            val repositories = mutableListOf<String>()
            listOf("reps", "repositories").forEach {
                if (json.has(it)) {
                    try {
                        val reps = json.getJSONArray(it)
                        repositories.addAll(reps.filter { it is String }.map { it.toString() })
                    } catch (e: JSONException) {
                        log(TEMPLATE_INVALID_PROPERTY.format(id, it))
                        log(e)
                    }
                }
            }

            val platform: Platform = if (json.has("platform")) {
                try {
                    when (json.getString("platform").toLowerCase()) {
                        "mobile" -> Platform.MOBILE
                        "windows" -> Platform.WINDOWS
                        "linux" -> Platform.LINUX
                        "mac" -> Platform.MAC
                        "android" -> Platform.ANDROID
                        "ios" -> Platform.IOS
                        else -> DEFAULT_PLATFORM
                    }
                } catch (e: JSONException) {
                    log(TEMPLATE_INVALID_PROPERTY.format(id, "platform"))
                    log(e)
                    DEFAULT_PLATFORM
                }
            } else {
                DEFAULT_PLATFORM
            }

            val manifest = parseManifest(id, json)
            return PluginConfig.fromManifest(manifest, dependencies.toSet(), repositories.toSet(), platform)
        }


        object AuthorParser {
            enum class Part { NAME, EMAIL, WEBSITE, PART_ENDED }

            fun parse(authorStr: String): Author {
                var part = Part.NAME
                val name = StringBuilder()
                val email = StringBuilder()
                val website = StringBuilder()
                for (char in authorStr) {
                    when {
                        char == '<'                -> part = Part.EMAIL
                        char == '>' || char == ')' -> part = Part.PART_ENDED
                        char == '('                -> part = Part.WEBSITE
                        part == Part.NAME          -> name.append(char)
                        part == Part.EMAIL         -> email.append(char)
                        part == Part.WEBSITE       -> website.append(char)
                    }
                }

                val (nameStr, emailStr, websiteStr) = listOf(name, email, website).map {
                    val s = it.toString().trim()
                    if (s.isEmpty()) {
                        null
                    } else {
                        s
                    }
                }
                return if (nameStr != null) {
                    Author(nameStr, emailStr, websiteStr)
                } else {
                    UNKNOWN_AUTHOR
                }
            }
        }



        fun getLocalString(strings: Map<String, String>): String? {
            val full_lang = Locale.getDefault().toLanguageTag()?.replace('-', '_')
            return if (full_lang != null) {
                val base_lang = if (full_lang.length > 2) full_lang.substring(0..1) else null
                strings[full_lang] ?: strings[base_lang] ?: strings[DEFAULT_LANGUAGE_KEY]
            } else {
                strings[DEFAULT_LANGUAGE_KEY]
            }
        }


        fun <K, V> Map<K, V>.getNotNullOrDefault(key: K, defaultValue: V) = this.getOrDefault(key, defaultValue) ?: defaultValue
    */
}
