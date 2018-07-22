package info.deskchan.external_loader.streams

import info.deskchan.external_loader.wrappers.MessageWrapper

interface ExternalStream {

    fun start()

    fun read(wrapper: MessageWrapper) : MessageWrapper.Message

    fun canRead() : Boolean

    fun readError() : String

    fun canReadError() : Boolean

    fun write(message: MessageWrapper.Message, wrapper: MessageWrapper)

    fun isAlive() : Boolean

    fun close()

}