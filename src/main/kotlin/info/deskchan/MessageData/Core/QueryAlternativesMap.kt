package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Get all alternatives
 *
 * <b>Response type</b>: Map<String, List<Map<String, Object>>>
 *
 * Alternative tag -> List of listeners:
 *   Listener tag
 *   Plugin name
 *   Listener priority
 *
 * **/

@MessageData.Tag("core:query-alternatives-map")
@MessageData.RequiresResponse
class QueryAlternativesMap : MessageData {
    companion object {
        val ResponseFormat: Map<String, List<Map<String, Any>>>? = null
    }
}