package info.deskchan.groovy_support

import groovy.lang.GroovyShell
import info.deskchan.core.MessageListener
import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxyInterface
import org.codehaus.groovy.control.CompilerConfiguration
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*
import java.util.regex.Pattern

class ScenarioPlugin : Plugin {

    internal var scenarioThread: Thread? = null
    internal var currentScenario: Scenario? = null

    internal var selected: String? = null

    override fun initialize(pluginProxyInterface: PluginProxyInterface): Boolean {

        pluginProxy = pluginProxyInterface
        pluginProxy.setResourceBundle("info/deskchan/groovy_support/strings")
        pluginProxy.setConfigField("name", pluginProxy.getString("scenario-plugin-name"))
        pluginProxy.getProperties().load()

        pluginProxy.sendMessage("core:add-command",  HashMap<String, Any>().apply {
            put("tag", "scenario:run-scenario")
            put("info", pluginProxy.getString("scenario-command.info"))
            put("msgInfo", pluginProxy.getString("scenario-command.msg-info"))
        })

        pluginProxy.addMessageListener("scenario:run-scenario", MessageListener { sender, tag, dat ->
            var path: String? = null
            if (dat is String)
                path = dat
            else if (dat is Map<*, *>) {
                val map = dat as Map<*, *>
                path = map["path"] as String
            }
            if (path == null)
                path = selected

            if (path == null) {
                pluginProxy.log("Path not specified for scenario")
                return@MessageListener
            }

            if (currentScenario != null)
                stopScenario()

            currentScenario = createScenario(path)
            runScenario()
        })

        pluginProxy.sendMessage("gui:set-panel", HashMap<String, Any>().apply {
            put("id", "scenario")
            put("name", pluginProxy.getString("scenario"))
            put("type", "submenu")
            put("action", "set")
            val list = LinkedList<HashMap<String, Any>>()
            list.add(HashMap<String, Any>().apply {
                put("id", "path")
                put("type", "AssetsManager")
                put("folder","scenarios")
                put("label", pluginProxy.getString("file"))
                put("onChange","scenario:selected")
            })
            val blist = LinkedList<HashMap<String, Any>>()
            blist.add(HashMap<String, Any>().apply {
                put("id", "start")
                put("type", "Button")
                put("value", pluginProxy.getString("start"))
                put("msgTag", "scenario:run-scenario")
            })
            blist.add(HashMap<String, Any>().apply {
                put("id", "stop")
                put("type", "Button")
                put("value", pluginProxy.getString("stop"))
                put("msgTag", "scenario:stop")
            })
            list.add(HashMap<String, Any>().apply {
                put("elements", blist)
            })
            put("controls", list)
        })

        pluginProxy.addMessageListener("scenario:selected", MessageListener{ sender, tag, data ->
            selected = data.toString()
        })
        pluginProxy.addMessageListener("scenario:stop", MessageListener { sender, tag, dat ->
            stopScenario()
        })

        if (pluginProxy.getProperties().getInteger("start.run_first", 0) < 2){
            currentScenario = createScenario("start_scenario.txt")
            runScenario()
        }


        return true
    }

    fun runScenario() {
        if (currentScenario != null) {
            if (scenarioThread != null)
                scenarioThread!!.interrupt()

            scenarioThread = object : Thread() {
                override fun run() {
                    try {
                        currentScenario!!.run()
                    } catch (e: InterruptedScenarioException){
                    } catch (e: Throwable){
                        pluginProxy.log(e)
                    }
                    scenarioThread = null
                }
            }
            scenarioThread!!.start()
        } else
            pluginProxy.sendMessage("DeskChan:request-say", "ERROR")
    }

    fun stopScenario(){
        try {
            scenarioThread?.interrupt()
            currentScenario?.quit()
            currentScenario = null
        } catch (e: Exception){ }
    }

    companion object {
        internal lateinit var pluginProxy: PluginProxyInterface

        private fun findNextBracket(line: String, start: Int): Int {
            var end: Int
            var level = 0
            end = start
            while (end < line.length && !(line[end] == ')' && level == 0)) {
                if (line[end] == '(' || line[end] == '{' || line[end] == '[')
                    level++
                else if (line[end] == ')' || line[end] == '}' || line[end] == ']') level--
                end++
            }
            return end
        }

        fun createScenario(pathString: String): Scenario? {
            val compilerConfiguration = CompilerConfiguration()
            compilerConfiguration.sourceEncoding = "UTF-8"
            compilerConfiguration.scriptBaseClass = "info.deskchan.groovy_support.Scenario"
            var path = File(pathString)
            if (!path.isAbsolute)
                path = pluginProxy.assetsDirPath.resolve("scenarios").resolve(pathString).toFile()
            compilerConfiguration.setClasspath(path.parent.toString())
            val groovyShell = GroovyShell(compilerConfiguration)
            var scriptLines: MutableList<String>? = null
            try {
                scriptLines = Files.readAllLines(path.toPath(), Charset.forName("UTF-8"))
            } catch (e: Exception) {
                pluginProxy.log("Invalid path specified for scenario")
                return null
            }

            val scriptText = StringBuilder()
            val buffers = arrayOfNulls<String>(1)
            for (u in buffers.indices) buffers[u] = null

            (0 until scriptLines.size).forEach {
                val line = scriptLines[it].trim { it <= ' ' }
                if (line.length == 0) return@forEach
                when (line[0]) {
                    '<' -> scriptLines[it] = line.substring(1).trim { it <= ' ' } + " = receive()"
                    '>' -> scriptLines[it] = "say(\"" + line.replace("\"","\\\"").substring(1).trim { it <= ' ' } + "\")"
                    '$' -> {
                        val matches = ArrayList<String>()
                        val m = Pattern.compile("([\"'])(?:(?=(\\\\?))\\2.)*?\\1|[^\\s]+").matcher(line.substring(1))
                        while (m.find() && matches.size < 4) {
                            matches.add(m.group())
                        }
                        if (matches.size == 0) {
                            scriptLines.removeAt(it)
                            return@forEach
                        }
                        val sb = StringBuilder("sendMessage(")
                        for (u in matches.indices) {
                            val arg = matches[u]
                            if (arg[0] != '"' && arg[0] != '\'') {
                                if (u > 0)
                                    sb.append(arg.replace("\"", "\\\""))
                                else
                                    sb.append('"'.toString() + arg.replace("\"", "\\\"") + '"'.toString())
                            } else
                                sb.append(arg)
                            sb.append(',')
                        }
                        sb.deleteCharAt(sb.length - 1)
                        sb.append(')')
                        scriptLines[it] = sb.toString()
                    }
                }
                scriptText.append(scriptLines[it])
                scriptText.append("\n")
            }
            val script = groovyShell.parse(scriptText.toString())
            return script as Scenario
        }

        class InterruptedScenarioException:RuntimeException()
    }
}
