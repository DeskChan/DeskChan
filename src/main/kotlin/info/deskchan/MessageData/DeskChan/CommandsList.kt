package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData

/**
 * Request commands list.
 * At standard edition it shows windows on screen with speech commands, but behavior can be changed with alternatives.
 * **/

@MessageData.Tag("DeskChan:commands-list")
class CommandsList : MessageData