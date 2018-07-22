package info.deskchan.external_loader.streams

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import info.deskchan.external_loader.wrappers.MessageWrapper
import java.io.IOException
import java.net.InetSocketAddress


class HTTPStream : ExternalStream, HttpHandler {

    var server: HttpServer?
    val key: String?

    constructor(context: String, port: Int, key: String?){
        this.key = key
        server = HttpServer.create(InetSocketAddress("localhost", port), 0)
        server!!.createContext(context, this)
        server!!.executor = null
    }
    override fun start(){
        server?.start()
    }

    var lastExchange: HttpExchange? = null

    @Throws(IOException::class)
    override fun handle(t: HttpExchange) {
        if (key != null && t.requestHeaders.getFirst(key) != key){
            t.sendResponseHeaders(401, 0)
            val os = t.responseBody
            os.close()
            return
        }
        if (t.requestMethod == "GET") {
            val response = MessageWrapper.serialize(buffer).toString().toByteArray()
            t.sendResponseHeaders(200, response.size.toLong())
            val os = t.responseBody
            os.write(response)
            os.close()
            buffer.clear()
            return
        }
        println("received "+t.requestMethod)
        lastExchange = t
    }

    override fun read(wrapper: MessageWrapper) : MessageWrapper.Message {
        while (lastExchange == null){
            if (!isAlive()) throw IOException("Process stopped working by unknown reason")
            Thread.sleep(2000)
        }
        return wrapper.unwrap(lastExchange!!.requestBody.reader(Charsets.UTF_8).readText())
    }

    override fun canRead() : Boolean = (lastExchange != null)

    override fun readError() : String = ""

    override fun canReadError() : Boolean = false

    private val buffer = mutableListOf<Any>()
    override fun write(message: MessageWrapper.Message, wrapper: MessageWrapper){
        val data = wrapper.wrap(message)
        if (message.type != "confirm"){
            buffer.add(data)
            return
        }
        if (lastExchange == null) throw Exception("No request to response.")
        val ex = lastExchange!!
        lastExchange = null
        val response = data.toString().toByteArray()
        ex.sendResponseHeaders(200, response.size.toLong())
        val os = ex.responseBody
        os.write(response)
        os.close()
    }

    override fun isAlive() : Boolean = (server != null)

    override fun close(){
        server?.stop(0)
    }

}