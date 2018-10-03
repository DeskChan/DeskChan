package info.deskchan.core

import java.io.*
import java.net.URI
import java.util.ArrayList

class Path : File {

    constructor(parent: String?): super(parent) 
    
    constructor(parent: String?, child: String?): super(parent, child)

    constructor(uri: URI): super(uri)

    constructor(file: File): super(file.toURI())
    
    fun resolve(child: String) : Path {
        return Path(this.path, child)
    }

    fun startsWith(string: String) : Boolean {
        val p1 = this.path.toLowerCase().replace('\\', '/')
        val p2 = string.toLowerCase().replace('\\', '/')
        return p1.startsWith(p2)
    }

    fun endsWith(string: String) : Boolean {
        val p1 = this.path.toLowerCase().replace('\\', '/')
        val p2 = string.toLowerCase().replace('\\', '/')
        return p1.endsWith(p2)
    }

    fun getParentPath(): Path? {
        return if (parent != null) Path(parent) else null
    }

    override fun listFiles(filter: FilenameFilter?): Array<File>? {
        throw NotImplementedError("Please, use files")
    }

    fun files(filter: FilenameFilter?): Set<Path> {
        val ss = list() ?: return setOf()
        val files = ArrayList<Path>()
        for (s in ss)
            if (filter == null || filter.accept(this, s))
                files.add(Path(this.path, s))
        return files.toSet()
    }

    fun files(): Set<Path> {
        val ss = list() ?: return setOf()
        val files = ArrayList<Path>()
        for (s in ss)
            files.add(Path(this.path, s))
        return files.toSet()
    }

    @Throws(IOException::class)
    fun newBufferedReader(): BufferedReader = newBufferedReader("UTF-8")

    @Throws(IOException::class)
    fun newBufferedReader(cs: String = "UTF-8"): BufferedReader {
        val reader = InputStreamReader(FileInputStream(this), cs)
        return BufferedReader(reader)
    }

    @Throws(IOException::class)
    fun newBufferedWriter(): BufferedWriter = newBufferedWriter("UTF-8")

    @Throws(IOException::class)
    fun newBufferedWriter(cs: String = "UTF-8"): BufferedWriter {
        val writer = OutputStreamWriter(FileOutputStream(this), cs)
        return BufferedWriter(writer)
    }

    fun relativize(other: Path) : String {
        if (startsWith(other))
            return path.removePrefix(other.path)
        else
            return path
    }

    fun resolveSibling(var1: String?): Path {
        if (var1 == null) {
            throw NullPointerException()
        } else {
            val var2: Path? = this.getParentPath()
            return if (var2 == null) Path(var1) else var2.resolve(var1)
        }
    }

    @Throws(IOException::class)
    fun readAllLines(): List<String> = newBufferedReader().readLines()

    @Throws(IOException::class)
    fun readAllBytes(): ByteArray = FileInputStream(this).readBytes()

}