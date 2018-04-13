package info.deskchan.external_loader

import info.deskchan.core.MessageListener
import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxyInterface
import info.deskchan.core.ResponseListener
import org.jetbrains.annotations.Nullable
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

class ExternalPlugin(private val pluginFile: File) : Plugin {

   private lateinit var processThread: Thread
   private lateinit var process: Process
   private lateinit var pluginProxy: PluginProxyInterface
   private lateinit var input: BufferedReader
   private lateinit var output: BufferedWriter
   private lateinit var err: BufferedReader

   override fun initialize(pluginProxy: PluginProxyInterface):Boolean {
      this.pluginProxy = pluginProxy
      println("type: "+pluginProxy.getConfigField("type"))
      val processBuilder = when(pluginProxy.getConfigField("type")){
         "Python" -> java.lang.ProcessBuilder("python", pluginFile.absolutePath)
         else -> null
      }
      if (processBuilder == null) return false

      println(pluginProxy.pluginDirPath)
      processBuilder.directory(pluginProxy.pluginDirPath.toFile())
      processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)

      process = processBuilder.start()

      input = process.inputStream.bufferedReader(Charsets.UTF_8)
      output = process.outputStream.bufferedWriter(Charsets.UTF_8)
      err = process.errorStream.bufferedReader(Charsets.UTF_8)

      write("info "+serialize(mapOf(
          "id" to pluginProxy.getId(),
          "dataDirPath"   to   pluginProxy.dataDirPath.toAbsolutePath().toString(),
          "pluginDirPath" to pluginProxy.pluginDirPath.toAbsolutePath().toString(),
          "assetsDirPath" to pluginProxy.assetsDirPath.toAbsolutePath().toString(),
          "rootDirPath"   to   pluginProxy.rootDirPath.toAbsolutePath().toString()
      )))
      processThread = Thread{
         checkThread()
      }
      processThread.start()
      return true
   }

   fun checkThread() {
      while (true){
         Thread.sleep(50)
         if (!process.isAlive) {
            pluginProxy.log(Exception("Process stopped working by unknown reason"))
            return
         }

         if (err.ready()) {
            val errline = err.readText()
            pluginProxy.log(errline)
         }
         if (!input.ready()) continue
         val line = input.readLine()

         if (line.startsWith("msg")){
            val msg = extract(line, 2)[1]
            val lines = readToMessageEnd()
            val res:Any
            if (lines.size > 0){
               if (lines.size > 1){
                  if (lines.size > 2)
                     res = pluginProxy.sendMessage(msg, deserialize(extract(lines[0], 2)[1]),
                             ExternListener(extract(lines[1], 2)[1].toInt()),
                             ExternListener(extract(lines[2], 2)[1].toInt())
                     )
                  else
                     res = pluginProxy.sendMessage(msg, deserialize(extract(lines[0], 2)[1]),
                                             ExternListener(extract(lines[1], 2)[1].toInt())
                     )
               } else
                  res = pluginProxy.sendMessage(msg, deserialize(extract(lines[0], 2)[1]))
            } else
               res = pluginProxy.sendMessage(msg, null)
            write("done " + res)
            continue
         }

         if (line.startsWith("add")){
            val msg = extract(line, 2)
            pluginProxy.addMessageListener(msg[1], ExternListener(msg[2].toInt()))
            write("done")
            continue
         }

         if (line.startsWith("rm")){
            val msg = extract(line, 2)
            pluginProxy.removeMessageListener(msg[1], ExternListener(msg[2].toInt()))
            listeners.remove(msg[2].toInt())
            write("done")
            continue
         }

         if (line.startsWith("timer")){
            val msg = extract(line, 2)
            val res = pluginProxy.setTimer(msg[1].toLong(), msg[2].toInt(), ExternListener(msg[3].toInt()))
            write("done " + res)
            continue
         }

         if (line.startsWith("Xtimer")){
            val msg = extract(line, 2)
            pluginProxy.cancelTimer(msg[1].toInt())
            listeners.remove(msg[1].toInt())
            write("done")
            continue
         }

         if (line.startsWith(">prop")){
            val msg = extract(line, 2)
            val res = pluginProxy.getProperties().get(msg[1])
            write("done " + res)
            continue
         }

         if (line.startsWith("<prop")){
            val msg = extract(line, 2)
            pluginProxy.getProperties().set(msg[1], msg[2])
            write("done")
            continue
         }

         if (line.startsWith(">conf")){
            val msg = extract(line, 2)
            val res = pluginProxy.getConfigField(msg[1])
            write("done " + res)
            continue
         }

         if (line.startsWith("<conf")){
            val msg = extract(line, 2)
            pluginProxy.setConfigField(msg[1], msg[2])
            write("done")
            continue
         }

         if (line.startsWith(">str")){
            val msg = extract(line, 2)
            write("done " + pluginProxy.getString(msg[1]))
            continue
         }

         if (line.startsWith("err")){
            val msg = extract(line, 2)
            val error:Map<String, Any?> = deserialize(msg[1]) as Map<String, Any?>
            pluginProxy.sendMessage("core-events:error", mapOf("class" to error["c"], "message" to error["m"], "stacktrace" to error["t"]))
            write("done")
            continue
         }

         if (line.startsWith("log")){
            val msg = extract(line, 2)
            pluginProxy.log(msg[1])
            write("done")
            continue
         }

         pluginProxy.log(Exception("Unknown command received: \"" + line + "\""))
      }
   }

   fun readToMessageEnd():List<String> {
      var line:String? = input.readLine()
      val lines = mutableListOf<String>()
      while (line != null && line != "#") {
         lines.add(line)
         line = input.readLine()
      }
      return lines
   }

   fun write(data:Any?){
      if (!process.isAlive) return

      println(process.isAlive.toString() + " " + input.ready() + " " + err.ready() + " " + data.toString())
      output.write(serialize(data) + "\r\n")
      output.flush()

   }

   override fun unload(){
      write("unload")
      pluginProxy.log("Waiting for process exit")
      process.waitFor()
      pluginProxy.getProperties().save()
   }

   /* Serialization */

   fun serialize(data:Any?):String? =
           when(data){
              is Collection<*> -> dataToJSON(data)
              is Map<*,*> -> dataToJSON(data)
              is Nullable -> null
              else -> data.toString()
           }

   fun dataToJSON(data:Map<*,*>):String {
      val obj: JSONObject = JSONObject()
      data.entries.forEach { obj.put(it.key.toString(), serialize(it.value)) }
      return obj.toString()
   }

   fun dataToJSON(data:Collection<*>):String {
      val obj: JSONArray = JSONArray()
      data.forEach { obj.put(serialize(it)) }
      return obj.toString()
   }

   /* DeSerialization */

   fun deserialize(data:Any?):Any? {
      if (data == null) return null
      if (data is List<*> || data is Map<*,*>) return data

      if (data is JSONArray) return JSONToData(data)
      if (data is JSONObject) return JSONToData(data)
      try {
         return JSONToData(JSONArray(data.toString()))
      } catch (e:Exception){ }
      try {
         return JSONToData(JSONObject(data.toString()))
      } catch (e:Exception){ }
      try {
         return data.toString().toDouble()
      } catch (e:Exception){ }
      try {
         return data.toString().toInt()
      } catch (e:Exception){ }
      try {
         if (data.toString().toLowerCase() == "true")  return true
         if (data.toString().toLowerCase() == "false") return false
      } catch (e:Exception){ }
      return data.toString()
   }


   fun JSONToData(data: JSONObject):Any? {
      val obj = mutableMapOf<String, Any?>()
      data.toMap().forEach { obj.put(it.key.toString(), deserialize(it.value)) }
      return obj
   }

   fun JSONToData(data: JSONArray):Any? {
      val obj = mutableListOf<Any?>()
      data.forEach { obj.add(deserialize(it)) }
      return obj
   }

   /* Helping */

   fun extract(line:String, count:Int) = (line as java.lang.String).split(" ", count)

   val listeners = mutableMapOf<Int, ExternListener>()
   inner class ExternListener(private val seq:Int) : ResponseListener, MessageListener {

      init {
         listeners.put(seq, this)
      }
      override fun handle(sender:String, data: Any?){
         write("call")
         write("sender "+sender)
         write("data "+serialize(data))
         write("#")
      }

      override fun handleMessage(sender: String?, tag: String?, data: Any?) {
         write("call")
         write("sender "+sender)
         write("tag "+tag)
         write("data "+serialize(data))
         write("#")
      }
   }

}