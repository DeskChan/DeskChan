package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Get current language tag
 *
 * <b>Response type</b>: String
 * **/

@MessageData.Tag("core:get-language")
@MessageData.RequiresResponse
class GetLanguage : MessageData {
    companion object {
        val ResponseFormat: String? = null
    }
}