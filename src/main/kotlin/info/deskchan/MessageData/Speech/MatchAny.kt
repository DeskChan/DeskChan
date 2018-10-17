package info.deskchan.MessageData.Speech

import info.deskchan.core.MessageData

/**
 * Check if speech matches to the rule
 *
 * @property speech Speech to match
 * @property rules Rules to match
 */
@MessageData.Tag("speech:match-any")
class MatchAny(val speech: String, val rules: List<String>) : MessageData