package info.deskchan.MessageData.Talk

import info.deskchan.core.MessageData

/**
 * Sends every time character preset was updated
 *
 * @property info Character representation
 * **/

@MessageData.Tag("talk:character-updated")
class CharacterUpdated(val character: Map<String, Any?>) : MessageData