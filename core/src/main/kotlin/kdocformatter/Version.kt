package kdocformatter

import java.io.BufferedInputStream
import java.util.Properties

object Version {
    var versionString: String
    init {
        val properties = Properties()
        val stream = Version::class.java.getResourceAsStream("/version.properties")
        BufferedInputStream(stream).use { buffered -> properties.load(buffered) }
        versionString = properties.getProperty("buildVersion")
    }
}
