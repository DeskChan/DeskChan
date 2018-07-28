package info.deskchan.external_loader.streams

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import info.deskchan.external_loader.wrappers.MessageWrapper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL


class HTTPStream : ExternalStream, HttpHandler {

    var server: HttpServer?
    val key: String?
    val wrapper: MessageWrapper

    constructor(context: String, port: Int, key: String?, wrapper: MessageWrapper){
        this.key = key
        this.wrapper = wrapper
        server = HttpServer.create(InetSocketAddress("localhost", port), 0)
        server!!.createContext(context, this)
        server!!.executor = null
    }
    override fun start(){
        server?.start()
    }

    private lateinit var info: MessageWrapper.Message
    private var lastExchange: HttpExchange? = null
    private val registeredListeners = mutableMapOf<String, MutableList<MessageWrapper.Message>>()
    private val sendToServerListeners = mutableMapOf<String, URL>()

    @Throws(IOException::class)
    override fun handle(t: HttpExchange) {
        // Not authorized
        if (key != null && t.requestHeaders.getFirst("Authorization") != key){
            t.sendResponseHeaders(401, 0)
            val os = t.responseBody
            os.close()
            return
        }
        if (t.requestMethod == "GET") {
            val list = mutableListOf<MessageWrapper.Message>()
            if ("Expect" in t.requestHeaders){
                t.requestHeaders["Expect"]!!.forEach {
                    if (registeredListeners.containsKey(it)) {
                        list.addAll(registeredListeners[it]!!)
                        registeredListeners.remove(it)
                    }
                }
            } else {
                info.additionalArguments["called"] = registeredListeners.keys
                list.add(info)
            }
            val response = wrapper.serialize(list).toString().toByteArray()
            t.sendResponseHeaders(200, response.size.toLong())
            val os = t.responseBody
            os.write(response)
            os.close()
            return
        }
        if (t.requestMethod == "POST")
            lastExchange = t
        else {
            t.sendResponseHeaders(400, 0)
            t.responseBody.close()
        }
    }

    override fun read(wrapper: MessageWrapper) : MessageWrapper.Message {
        while (lastExchange == null){
            if (!isAlive()) throw IOException("Process stopped working by unknown reason")
            Thread.sleep(2000)
        }
        val message = wrapper.unwrap(lastExchange!!.requestBody.reader(Charsets.UTF_8).readText())

        if ("sendTo" in message.additionalArguments){
            when (message.type){
                "addMessageListener" -> {
                    sendToServerListeners[message.getRequiredAsString(1)] =
                            URL(message.additionalArguments["sendTo"].toString())
                }
                "sendMessage" -> {
                    if ("response" in message.additionalArguments)
                        sendToServerListeners[message.additionalArguments["response"].toString()] =
                                URL(message.additionalArguments["sendTo"].toString())
                    if ("return" in message.additionalArguments)
                        sendToServerListeners[message.additionalArguments["return"].toString()] =
                                URL(message.additionalArguments["sendTo"].toString())
                }
                "setTimer" -> {
                    sendToServerListeners[message.getRequiredAsString(2)] =
                            URL(message.additionalArguments["sendTo"].toString())
                }
            }
        }

        return message
    }

    override fun canRead() : Boolean = (lastExchange != null)

    override fun readError() : String = ""

    override fun canReadError() : Boolean = false

    override fun write(message: MessageWrapper.Message, wrapper: MessageWrapper){
        val data = wrapper.wrap(message)
        when (message.type){
            "setInfo" -> {
                info = message
            }
            "confirm" -> {
                if (lastExchange == null) throw Exception("No request to response.")
                val ex = lastExchange!!
                lastExchange = null
                val response = data.toString().toByteArray()
                ex.sendResponseHeaders(200, response.size.toLong())
                val os = ex.responseBody
                os.write(response)
                os.close()
            }
            "call" -> {
                val tag = message.getRequiredAsString(0)
                if (tag in sendToServerListeners){
                    val url = sendToServerListeners[tag]!!
                    println(url)
                    val con = url.openConnection() as HttpURLConnection
                    con.doOutput = true
                    con.requestMethod = "POST"
                    println(data.toString())
                    val writer = con.outputStream.writer(Charsets.UTF_8)
                    writer.write(data.toString())
                    writer.flush()
                    writer.close()
                    val code = con.responseCode
                    con.disconnect()
                } else if (tag in registeredListeners)
                    registeredListeners[tag]!!.add(message)
                else
                    registeredListeners[tag] = mutableListOf(message)
            }
            "unload" -> {
                // TODO:  TOO LAZY
            }
        }
    }

    override fun isAlive() : Boolean = (server != null)

    override fun close(){
        server?.stop(0)
    }

}