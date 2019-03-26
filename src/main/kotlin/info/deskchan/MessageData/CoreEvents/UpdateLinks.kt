package info.deskchan.MessageData.CoreEvents

import info.deskchan.MessageData.Core.EventLink
import info.deskchan.core.MessageData

/**
 * Links list for event was updated
 *
 * Message is sending as two instances: "core-events:update-links" and "core-events:update-links:%eventname%"
 * First message will be sent when any event link is updated. Data will be null
 * Second message will be sent when any event link subscribed to %eventname% is updated. Data will contain all links currently subscribed to %eventname%.
 *
 */
@MessageData.Tag("core-events:update-links")
class UpdateLinks : ArrayList<EventLink>()