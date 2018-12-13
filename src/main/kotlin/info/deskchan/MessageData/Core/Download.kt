package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData
import info.deskchan.core.Path
import java.net.URI

/**
 * Download file
 *
 * @property url URL where file is stored
 * @property path Path where to save file. Will be saved in standard location if no path provided.
 * @property filename Filename for file
 */
@MessageData.Tag("core:download")
class Download : MessageData {

    var url: String
    var path: String
    var filename: String

    constructor(url: String, path: Path){
        this.url = url
        this.path = path.parent
        this.filename = path.name
    }

    constructor(url: String, path: String, filename: String){
        this.url = url
        this.path = path
        this.filename = filename
    }

    constructor(url: URI, path: Path){
        this.url = url.toString()
        this.path = path.parent
        this.filename = path.name
    }

    constructor(url: URI, path: String, filename: String){
        this.url = url.toString()
        this.path = path
        this.filename = filename
    }

}