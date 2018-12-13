package info.deskchan.MessageData.Speech

import info.deskchan.core.MessageData

/**
 * Check if speech matches to the rule
 *
 * @property speech Speech to match
 * @property rule Rule to match
 */
@MessageData.Tag("speech:match")
class Match(val speech: String, val rule: String) : MessageData