package info.deskchan.external_loader.wrappers

import java.security.InvalidKeyException

class InlineMessageWrapper : MessageWrapper() {

    override fun wrap(message: MessageWrapper.Message): Any {
        val s = StringBuilder(message.type)
        message.requiredArguments.forEach {
            s.append(" ")
            s.append(serialize(it))
        }
        s.append("\n")
        message.additionalArguments.forEach { t, u ->
            s.append(t); s.append(" "); s.append(serialize(u)); s.append("\n")
        }
        return s
    }

    override fun unwrap(text: String): MessageWrapper.Message {
        val lines = mutableListOf<String>()
        text.split("\n").forEach { if (it.isNotBlank()) lines.add(it) }

        var data = (lines[0] as java.lang.String).split(" ", 2)

        val type = data[0].trim()
        val argsCount = messageArgsCount[type]
        if (argsCount == null)
            throw InvalidKeyException("Type of received message not correct.")

        val message = Message(type)

        if (argsCount > 0) {
            data = (data[1] as java.lang.String).split(" ", argsCount)
            data.forEach { message.requiredArguments.add(deserialize(it)) }
        }

        for(i in 1..lines.size-1){
            val line = (lines[i] as java.lang.String).split(" ", 2)
            message.additionalArguments.put(line[0], deserialize(line[1]))
        }

        return message
    }
}