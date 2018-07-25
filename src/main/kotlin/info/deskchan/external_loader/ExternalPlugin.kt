package info.deskchan.external_loader

import info.deskchan.core.MessageListener
import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxyInterface
import info.deskchan.core.ResponseListener
import info.deskchan.external_loader.streams.ExternalStream
import info.deskchan.external_loader.streams.HTTPStream
import info.deskchan.external_loader.streams.ProcessIOStream
import info.deskchan.external_loader.wrappers.InlineMessageWrapper
import info.deskchan.external_loader.wrappers.JSONMessageWrapper
import info.deskchan.external_loader.wrappers.MessageWrapper
import java.io.File
import java.io.IOException
import java.security.InvalidKeyException
import java.util.*

class ExternalPlugin(private val pluginFile: File) : Plugin {

   private lateinit var pluginProxy: PluginProxyInterface
   private lateinit var processThread: Thread
   private lateinit var stream: ExternalStream
   private lateinit var wrapper: MessageWrapper

   private var ping = 200L

   override fun initialize(pluginProxy: PluginProxyInterface):Boolean {
      this.pluginProxy = pluginProxy
      //println("type: "+pluginProxy.getConfigField("type"))
      when(pluginProxy.getConfigField("type")){
         "Python" -> {
            if (pluginFile.absolutePath.endsWith(".py2"))
               stream = ProcessIOStream(pluginFile, "python2")
            else if (pluginFile.absolutePath.endsWith(".py3"))
               stream = ProcessIOStream(pluginFile, "python3")
            else
               stream = ProcessIOStream(pluginFile, "python")
            wrapper = InlineMessageWrapper()
         }
         "HttpServer" -> {
            val file = Properties()
            file.load(pluginFile.reader())
            ping = file.getProperty("delay", ping.toString()).toLong()
            stream = HTTPStream(
                    file.getProperty("context", "/"),
                    file.getProperty("port", "3640").toInt(),
                    file.getProperty("key", null)
            )

            wrapper = JSONMessageWrapper()
         }
         else -> return false
      }

      try {
         stream.start()
      } catch (e: IOException){
         pluginProxy.log(IOException("Cannot run external plugin of type "+pluginProxy.getConfigField("type")+": "+e.message, e))
         return false
      }

      stream.write(MessageWrapper.Message(
          "setInfo",
              mutableListOf(),
              mutableMapOf(
                "id" to pluginProxy.getId(),
                "dataDirPath"   to   pluginProxy.dataDirPath.toAbsolutePath().toString(),
                "pluginDirPath" to pluginProxy.pluginDirPath.toAbsolutePath().toString(),
                "assetsDirPath" to pluginProxy.assetsDirPath.toAbsolutePath().toString(),
                "rootDirPath"   to   pluginProxy.rootDirPath.toAbsolutePath().toString()
              )
      ), wrapper)
      processThread = Thread{
         checkThread()
      }
      processThread.name = pluginProxy.getId() + " plugin thread"
      processThread.start()
      return true
   }


   private fun checkThread() {
      while (true){
         if (!stream.canRead() && !stream.canReadError()) {
             if (!stream.isAlive()) {
                 pluginProxy.log(Exception("Process stopped working by unknown reason"))
                 return
             }
             Thread.sleep(ping)
             continue
         }

         var message : MessageWrapper.Message
         try {
            println("waiting for another message")
            message = stream.read(wrapper)
            println("==== got: "+message.type)
         } catch (e: IOException){
            println("breaking with exception "+e)
            val error = stream.readError()
            pluginProxy.log(Exception(error))
            break
         } catch (e: InvalidKeyException){
            pluginProxy.log(e)
            continue
         } catch (e: Throwable){
            pluginProxy.log(e)
            break
         }

         when (message.type){
            "sendMessage" -> {
               val result: Any
               val args = message.requiredArguments
               val msg = args[0].toString()
               val ret = message.additionalArguments["return"]?.toString()
               val resp = message.additionalArguments["response"]?.toString()
               val data = args[1]
               if (resp != null){
                  if (ret != null)
                     result = pluginProxy.sendMessage(msg, data, ExternListener(resp), ExternListener(ret))
                  else
                     result = pluginProxy.sendMessage(msg, data, ExternListener(resp))
               } else
                  result = pluginProxy.sendMessage(msg, data)
               confirm(result)
            }
            "callNextAlternative" -> {
               pluginProxy.callNextAlternative(
                       message.getRequiredAsString(0),
                       message.getRequiredAsString(1),
                       message.getRequiredAsString(2),
                       message.requiredArguments[3]
               )
               confirm(null)
            }
            "addMessageListener" -> {
               pluginProxy.addMessageListener(
                       message.getRequiredAsString(0),
                       ExternListener(message.getRequiredAsString(1))
               )
               confirm(null)
            }
            "removeMessageListener" -> {
               val id = message.getRequiredAsString(1)
               if (id in listeners)
                  pluginProxy.addMessageListener(
                       message.getRequiredAsString(0),
                       listeners.remove(id)!!
                  )
               confirm(null)
            }
            "setTimer" -> {
               val listener = ExternListener(message.getRequiredAsString(2), false)
               val r = pluginProxy.setTimer(
                       message.getRequiredAsLong(0),
                       message.getRequiredAsInt(1),
                       listener
               )
               listeners["timer"+r] = listener
               confirm(r)
            }
            "cancelTimer" -> {
               val id = message.getRequiredAsInt(0)
               pluginProxy.cancelTimer(id)
               listeners.remove("timer"+id)
               confirm(null)
            }
            "setAlternative" -> {
               pluginProxy.setAlternative(
                       message.getRequiredAsString(0),
                       message.getRequiredAsString(1),
                       message.getRequiredAsInt(2)
               )
               confirm(null)
            }
            "deleteAlternative" -> {
               pluginProxy.deleteAlternative(
                       message.getRequiredAsString(0),
                       message.getRequiredAsString(1)
               )
               confirm(null)
            }
            "setProperty" -> {
               val last = when(message.requiredArguments[1]){
                  null -> pluginProxy.getProperties().remove(message.getRequiredAsString(0))
                  else -> pluginProxy.getProperties().put(
                           message.getRequiredAsString(0),
                           message.requiredArguments[1]!!
                  )
               }

               confirm(last)
            }
            "getProperty" -> {
               val ret = pluginProxy.getProperties().get(
                       message.getRequiredAsString(0)
               )

               confirm(ret)
            }
            "setConfigField" -> {
               val last = pluginProxy.setConfigField(
                       message.getRequiredAsString(0),
                       message.requiredArguments[1]!!
               )

               confirm(last)
            }
            "getConfigField" -> {
               val ret = pluginProxy.getConfigField(
                       message.getRequiredAsString(0)
               )

               confirm(ret)
            }
            "getString" -> {
               val ret = pluginProxy.getString(
                       message.getRequiredAsString(0)
               )

               confirm(ret)
            }
            "log" -> {
               pluginProxy.log(
                       message.getRequiredAsString(0)
               )
               confirm(null)
            }
            "error" -> {
               pluginProxy.sendMessage("core-events:error",
                  mapOf(
                       "class" to message.additionalArguments.getOrDefault("class", "Error"),
                       "message" to message.getRequiredAsString(0),
                       "stacktrace" to message.additionalArguments.getOrDefault("stacktrace", listOf("No traceback specified"))
                  ))
               confirm(null)
            }
            "initializationCompleted" -> {
               confirm(null)
            }
         }
      }
   }

   fun confirm(data: Any?){
      if (!stream.isAlive()) return

      val message = when (data){
         null,
         Unit -> MessageWrapper.Message("confirm")
         else -> MessageWrapper.Message("confirm", mutableListOf(data))
      }
      stream.write(message, wrapper)
   }

   override fun unload(){
      stream.write(MessageWrapper.Message("unload"), wrapper)
      pluginProxy.log("Waiting for process exit")
      stream.close()
      pluginProxy.getProperties().save()
   }

   /* Handling messages */

   val listeners = mutableMapOf<String, ExternListener>()
   open inner class ExternListener : ResponseListener, MessageListener {

      protected val seq:String

      constructor(seq:String, add:Boolean = true) {
         this.seq = seq
         if (add)
            listeners[seq] = this
      }

      override fun handle(sender:String, data: Any?){
         //println("-- handling1 " + seq + " " + sender + " " + data)
          val message = MessageWrapper.Message(
                  "call",
                  mutableListOf(seq, sender, data)
          )
          stream.write(message, wrapper)
      }

      override fun handleMessage(sender: String?, tag: String?, data: Any?) {
         //println("-- handling2 " + seq.toString() + " " + sender + " " + tag + " " + data)
          val message = MessageWrapper.Message(
                  "call",
                  mutableListOf(seq, sender, data),
                  mutableMapOf("tag" to tag)
          )
          stream.write(message, wrapper)
      }
   }

}