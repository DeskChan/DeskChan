import java.nio.file.Files
import java.nio.file.Path

final class Localization {

	static final Properties strings = new Properties()
	public static Object instance

	static String getString(String key) {
		String s
		try {
			s = strings.getProperty(key)
			if(s!=null)
				return new String(s.getBytes("ISO-8859-1"), "UTF-8")
			else return instance.getString(key)
		} catch (Exception e) {
			return key
		}
	}

	static void load() {
		Path resourcesPath = instance.getPluginDirPath().resolve("resources")
		def stringsPath = resourcesPath.resolve("strings_" + Locale.getDefault() + ".properties")
		if (!Files.isReadable(stringsPath)) {
			stringsPath = resourcesPath.resolve("strings.properties")
		}
		if (Files.isReadable(stringsPath)) {
			try {
				strings.load(Files.newInputStream(stringsPath))
			} catch (Exception e) {
				e.printStackTrace()
			}
		}
	}

}

