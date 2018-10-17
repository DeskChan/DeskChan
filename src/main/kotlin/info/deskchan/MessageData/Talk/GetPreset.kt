package info.deskchan.MessageData.Talk

import info.deskchan.core.MessageData

/**
 * Get current character preset
 *
 * <b>Response type</b>: Map<String, Object>>
 *
 *  - name - character name
 *  - phrases - phrases packs names
 *  - tags - tags specified
 *  - emotion - current active emotion
 *  - all features by their names
 *
 * **/

@MessageData.Tag("talk:get-preset")
@MessageData.RequiresResponse
class GetPreset : MessageData {
    companion object {
        val ResponseFormat: Map<String, Any>? = null
    }
}