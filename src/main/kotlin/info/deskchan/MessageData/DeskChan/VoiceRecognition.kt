package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Notification about user speech received by voice recognition module
 *
 * @property value Speech text
 * **/

@MessageData.Tag("DeskChan:voice-recognition")
class VoiceRecognition (val value: String) : MessageData