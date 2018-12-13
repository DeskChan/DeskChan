package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData
import info.deskchan.core.Path
import java.net.URI

/**
 * Open link. It can be file, directory or network URL.
 * Recommended to use this message instead of opening it by yourself, this message is OS-independent.
 *
 * @property value Link to open
 */
@MessageData.Tag("core:open-link")
class OpenLink(val value: String) : MessageData {

    constructor(file: Path) : this(file.absolutePath)

    constructor(url: URI) : this(url.toString())

}