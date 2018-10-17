package info.deskchan.MessageData.Talk

import info.deskchan.core.MessageData

/**
 * Request chat phrase. Phrase intent will be selected randomly from suitable intents list, including CHAT.
 * **/

@MessageData.Tag("talk:chat")
class Chat : MessageData