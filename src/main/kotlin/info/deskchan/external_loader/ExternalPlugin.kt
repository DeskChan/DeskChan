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
      //println("type: "+pluginProxy.getConfigField("type"))
      val processBuilder = when(pluginProxy.getConfigField("type")){
         "Python" -> {
            if (pluginFile.absolutePath.endsWith(".py2"))
               java.lang.ProcessBuilder("python2", pluginFile.absolutePath)
            else if (pluginFile.absolutePath.endsWith(".py3"))
               java.lang.ProcessBuilder("python3", pluginFile.absolutePath)
            else
               java.lang.ProcessBuilder("python", pluginFile.absolutePath)
         }
         else -> null
      }
      if (processBuilder == null) return false

      processBuilder.directory(pluginProxy.pluginDirPath.toFile())

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

   var buffer = mutableListOf<String>()
   fun checkThread() {
      while (true){
         Thread.sleep(50)

         if (!process.isAlive && !err.ready() && !input.ready()) {
            pluginProxy.log(Exception("Process stopped working by unknown reason"))
            return
         }

         var s:String
         try {
            //println("waiting for another message")
            s = input.readLine().trim()
            //println("==== got: "+s)
         } catch (e: Exception){
            //println("breaking with exception")
            val errline = err.readText()
            pluginProxy.log(Exception(errline))
            break
         }

         if (s.trim() != "#"){
            buffer.add(s)
            continue
         } else if (buffer.size == 0)
            continue

         val lines = buffer
         buffer = mutableListOf()
         val line = lines[0]
         //println("===== starting with: " + line)
         if (line.trim().isEmpty()) continue

         if (line.startsWith("msg")) {
            val msg = extract(line, 2)[1]
            val result: Any
            var data: Any? = null
            var ret: Int? = null
            var resp: Int? = null
            lines.forEach {
               if      (it.startsWith("data")) data = deserialize(extract(it, 2)[1])
               else if (it.startsWith("resp")) resp = extract(it, 2)[1].toInt()
               else if (it.startsWith("ret"))  ret  = extract(it, 2)[1].toInt()
            }
            //println(if (data != null) data.toString() else "null" + " " + ret + " " + resp)
            if (resp != null){
               if (ret != null)
                  result = pluginProxy.sendMessage(msg, data, ExternListener(resp!!), ExternListener(ret!!))
               else
                  result = pluginProxy.sendMessage(msg, data, ExternListener(resp!!))
            } else
               result = pluginProxy.sendMessage(msg, data)
            write("done " + result)
            continue
         }

         if (line.startsWith("add")) {
            val msg = extract(line, 3)
            pluginProxy.addMessageListener(msg[1], ExternListener(msg[2].toInt()))
            write("done")
            continue
         }

         if (line.startsWith("rm")) {
            val msg = extract(line, 3)
            pluginProxy.removeMessageListener(msg[1], ExternListener(msg[2].toInt()))
            listeners.remove(msg[2].toInt())
            write("done")
            continue
         }

         if (line.startsWith("timer")) {
            val msg = extract(line, 4)
            val res = pluginProxy.setTimer(msg[1].toLong(), msg[2].toInt(), ExternListener(msg[3].toInt()))
            write("done " + res)
            continue
         }

         if (line.startsWith("Xtimer")) {
            val msg = extract(line, 2)
            pluginProxy.cancelTimer(msg[1].toInt())
            listeners.remove(msg[1].toInt())
            write("done")
            continue
         }

         if (line.startsWith(">prop")) {
            val msg = extract(line, 2)
            val res = pluginProxy.getProperties().get(msg[1])
            write("done " + res)
            continue
         }

         if (line.startsWith("<prop")) {
            val msg = extract(line, 3)
            pluginProxy.getProperties().set(msg[1], msg[2])
            write("done")
            continue
         }

         if (line.startsWith(">conf")) {
            val msg = extract(line, 2)
            val res = pluginProxy.getConfigField(msg[1])
            write("done " + res)
            continue
         }

         if (line.startsWith("<conf")) {
            val msg = extract(line, 3)
            pluginProxy.setConfigField(msg[1], msg[2])
            write("done")
            continue
         }

         if (line.startsWith(">str")) {
            val msg = extract(line, 2)
            write("done " + pluginProxy.getString(msg[1]))
            continue
         }

         if (line.startsWith("err")) {
            val msg = extract(line, 2)
            val error: Map<String, Any?> = deserialize(msg[1]) as Map<String, Any?>
            pluginProxy.sendMessage("core-events:error", mapOf("class" to error["c"], "message" to error["m"], "stacktrace" to error["t"]))
            write("done")
            continue
         }

         if (line.startsWith("log")) {
            val msg = extract(line, 2)
            pluginProxy.log(msg[1])
            write("done")
            continue
         }

         if (line.startsWith("initcmpl")){
            write("done")
            continue
         }

         pluginProxy.log(Exception("Unknown command received: \"" + line + "\""))
      }
   }

   fun write(data:Any?){
      //println("alive: "+process.isAlive + " / " + data.toString())
      if (!process.isAlive) return

      output.write(serialize(data) + "\n")
      output.flush()
   }

   override fun unload(){
      write("unload")
      pluginProxy.log("Waiting for process exit")
      process.destroyForcibly()
      process.waitFor()
      pluginProxy.getProperties().save()
   }

   /* Serialization */

   fun serialize(data:Any?):String? =
           when(data){
              is Collection<*> -> dataToJSON(data)
              is Map<*,*> -> dataToJSON(data)
              is Nullable -> null
              else -> data.toString().replace("\n", "\t")
           }

   fun dataToJSON(data:Map<*,*>):String {
      val obj: JSONObject = JSONObject()
      data.entries.forEach { obj.put(it.key.toString().replace("\n", "\t"), serialize(it.value)) }
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
         return rdata.toInt()
      } catch (e:Exception){ }
      try {
         if (rdata.toLowerCase() == "true")  return true
         if (rdata.toLowerCase() == "false") return false
      } catch (e:Exception){ }
      return rdata
   }


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

   /* Helping */

   fun extract(line:String, count:Int) = (line as java.lang.String).split(" ", count)

   val listeners = mutableMapOf<Int, ExternListener>()
   inner class ExternListener(private val seq:Int) : ResponseListener, MessageListener {

      init {
         listeners.put(seq, this)
      }
      override fun handle(sender:String, data: Any?){
         //println("-- handling1 " + seq.toString() + " " + sender + " " + data)
         write("call "+seq)
         write("sender "+sender)
         write("data "+serialize(data))
         write("#")
      }

      override fun handleMessage(sender: String?, tag: String?, data: Any?) {
         //println("-- handling2 " + seq.toString() + " " + sender + " " + tag + " " + data)
         write("call "+seq)
         write("sender "+sender)
         write("tag "+tag)
         write("data "+serialize(data))
         write("#")
      }
   }

}