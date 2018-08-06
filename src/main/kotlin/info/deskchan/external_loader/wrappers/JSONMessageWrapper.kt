package info.deskchan.external_loader.wrappers

import org.jetbrains.annotations.Nullable
import org.json.JSONObject
import java.security.InvalidKeyException

class JSONMessageWrapper : MessageWrapper() {

    override fun wrap(message: MessageWrapper.Message): Any {
        val json = JSONObject()
        json.put("type", message.type)
        json.put("args", serialize(message.requiredArguments))
        message.additionalArguments.forEach { t, u -> json.put(t, serialize(u)) }
        return json
    }

    override fun unwrap(text: String): MessageWrapper.Message {
        println(text)
        val json = JSONObject(text)

        val type = json.remove("type")
        if (type == null || !messageArgsCount.containsKey(type))
            throw InvalidKeyException("Type of received message not correct.")

        val message = Message(type.toString())

        if (json.get("args") != null){
            val args = deserialize(json.get("args"))
            when(args){
                is Collection<*> -> args.forEach { message.requiredArguments.add(it) }
                is Map<*,*> -> args.forEach { k, v -> message.additionalArguments.put(k.toString(), v) }
                is Nullable, "null" -> false
                else -> message.requiredArguments.add(args)
            }
        }

        json.keySet().forEach { message.additionalArguments[it] = json.get(it).toString() }

        return message
    }

}