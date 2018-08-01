package info.deskchan.external_loader.streams

import info.deskchan.external_loader.wrappers.MessageWrapper
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException

class ProcessIOStream : ExternalStream {

    private val file: File
    private var process: Process? = null
    private val processBuilder: ProcessBuilder

    private lateinit var input: BufferedReader
    private lateinit var output: BufferedWriter
    private lateinit var err: BufferedReader

    var inputBuffer = mutableListOf<String>()

    constructor(file: File){
        this.file = file
        this.processBuilder = ProcessBuilder(file.absolutePath)
        this.processBuilder.directory(file.parentFile)
    }

    constructor(file: File, prefix: String){
        this.file = file
        this.processBuilder = ProcessBuilder(prefix, file.absolutePath)
        this.processBuilder.directory(file.parentFile)
    }

    override fun start() {
        process = processBuilder.start()
        input = process!!.inputStream.bufferedReader(Charsets.UTF_8)
        output = process!!.outputStream.bufferedWriter(Charsets.UTF_8)
        err = process!!.errorStream.bufferedReader(Charsets.UTF_8)
    }

    override fun canRead(): Boolean {
        return input.ready()
    }

    override fun read(wrapper: MessageWrapper) : MessageWrapper.Message {
        var line : String
        while (true) {
            if (!isAlive()) throw IOException("Process stopped working by unknown reason")
            if (!input.ready()){
                Thread.sleep(50)
                continue
            }

            line = input.readLine().trim()

            if (line == "#"){
                if (inputBuffer.size == 0) continue
                val data = StringBuilder()
                inputBuffer.forEach { data.append(it); data.append("\n") }
                inputBuffer.clear()
                return wrapper.unwrap(data.toString())
            }
            inputBuffer.add(line)
        }
    }

    override fun canReadError(): Boolean {
        return err.ready()
    }

    override fun readError(): String {
        var line : String
        while (true) {
            line = err.readLine().trim()

            if (!err.ready()){
                inputBuffer.add(line)
                if (inputBuffer.size == 0) continue
                val data = StringBuilder()
                inputBuffer.forEach { data.append(it); data.append("\n") }
                inputBuffer.clear()
                return data.toString()
            }
            inputBuffer.add(line)
        }
    }

    override fun write(message: MessageWrapper.Message, wrapper: MessageWrapper) {
        if (!isAlive()) return

        val data = wrapper.wrap(message).toString()
        output.write(data)
        output.write("#\n")
        output.flush()
    }

    override fun isAlive() : Boolean {
        return process != null && process!!.isAlive
    }

    override fun close() {
        process?.destroyForcibly()
        process?.waitFor()
    }

    override fun toString(): String {
        return "Process named: " + processBuilder.command() + ", at: " + processBuilder.directory()
    }

}