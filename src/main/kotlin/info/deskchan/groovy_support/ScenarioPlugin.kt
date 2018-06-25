package info.deskchan.groovy_support

import groovy.lang.GroovyShell
import info.deskchan.core.*
import org.codehaus.groovy.control.CompilerConfiguration
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*

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

        /* Run scenario.
         * Public message
         * Params: Map
         *           path:String - path to scenario
         *           giveOwnership:Any - is set, scenario will run with sender as owner
         *           msgData:Any - pass any additional data to scenario
         * Returns: None */
        pluginProxy.addMessageListener("scenario:run-scenario", MessageListener { sender, tag, dat ->
            var path: String? = null
            var owner: String = pluginProxy.getId()
            var data: Any? = null
            if (dat is String)
                path = dat
            else if (dat is Map<*, *>) {
                val map = dat as Map<*, *>
                path = map["path"] as String
                if (map["giveOwnership"] != null)
                    owner = sender
                data = map["msgData"]
            }
            if (path == null)
                path = selected

            if (path == null) {
                pluginProxy.log("Path not specified for scenario")
                return@MessageListener
            }

            if (currentScenario != null)
                stopScenario()

            currentScenario = createScenario(owner, path, data)
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
                put("moreURL", "https://forum.deskchan.info/category/19/scenarios");
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

        /* Stop current scenario
         * Public message
         * Params: None
         * Returns: None  */
        pluginProxy.addMessageListener("scenario:stop", MessageListener { sender, tag, dat ->
            stopScenario()
        })

        if (CoreInfo.getCoreProperties().getInteger("start.run_first", 0) < 2){
            currentScenario = createScenario("core", "start_scenario.txt", null)
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

        private val pairs = mapOf(' ' to ' ', '"' to '"','\'' to '\'', '[' to ']', '{' to '}', '(' to ')')
        private fun findNextBracket(line: String, start: Int): Int {
            var end: Int
            val stack = LinkedList<Char>()
            stack.add(line[start])
            end = start
            if (line[end] == ' ')
                while (line[end] == ' ') end++
            end += 1
            while (end < line.length && stack.size > 0) {
                if (line[end] == pairs[stack.last]){
                    stack.removeLast()
                } else if (line[end] != ' ' && pairs.keys.contains(line[end])){
                    stack.add(line[end])
                }
                end++
            }
            return end
        }

        fun createScenario(owner:String, pathString: String, data: Any?): Scenario? {
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
                pluginProxy.log(e)
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
                    '~' -> scriptLines[it] = "requestPhrase(\"" + line.replace("\"","\\\"").substring(1).trim { it <= ' ' } + "\")"
                    '$' -> {
                        val matches = ArrayList<String>()

                        var start = 1
                        var end: Int
                        do {
                            end = findNextBracket(line, start)
                            matches.add(line.substring(start, end))
                            start = end
                        } while (start < line.length)

                        matches[0] = "\"" + matches[0].trim() + "\""
                        scriptLines[it] = "sendMessage(" + matches.joinToString() + ")"
                    }
                }
                scriptText.append(scriptLines[it])
                scriptText.append("\n")
            }
            val script = groovyShell.parse(scriptText.toString()) as Scenario

            val pp = PluginManager.getInstance().getPlugin(owner)?: pluginProxy
            script.initialize(pp, data)

            return script
        }

        class InterruptedScenarioException:RuntimeException()
    }
}
