package info.deskchan.MessageData.DeskChan

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Distribute resources to plugins with instructions file
 *
 * @property file Instructions file
 * **/

@MessageData.Tag("core:distribute-resources")
class DistributeResources (var file: String) : MessageData {

    constructor(file: Path) : this(file.absolutePath)

}