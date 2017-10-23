package info.deskchan.core;

import org.apache.commons.lang3.SystemUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CoreInfo {
	
	private static final Map<String, String> info = new HashMap<>();
	
	public static String get(String key) {
		return info.getOrDefault(key, null);
	}
	
	public static Set<String> keys() {
		return info.keySet();
	}

	public static Map<String,String> locales;

	public static void printInfo() {
		PluginManager.log((!CoreInfo.get("NAME").equals("DeskChan") ? "Unofficial build: " : "")+CoreInfo.get("NAME") + " " + CoreInfo.get("VERSION"));
		PluginManager.log("Go to " + CoreInfo.get("PROJECT_SITE_URL") + " for more information");
		PluginManager.log("Git branch: " + CoreInfo.get("GIT_BRANCH_NAME"));
		PluginManager.log("Git commit hash: " + CoreInfo.get("GIT_COMMIT_HASH"));
		PluginManager.log("Build date and time: " + CoreInfo.get("BUILD_DATETIME"));
		PluginManager.log("Operation system: " + SystemUtils.OS_NAME+"-"+SystemUtils.OS_VERSION+", "+SystemUtils.USER_LANGUAGE+", Java ver.: "+SystemUtils.JAVA_VERSION);
	}
	
	static {
		try {
			Class cls = Class.forName("info.deskchan.core.BuildConfig");
			for (Field field : cls.getDeclaredFields()) {
				try {
					Object value = field.get(null);
					info.put(field.getName(), value.toString());
				} catch (IllegalAccessException e) {
					// Do nothing
				}
			}
		} catch (ClassNotFoundException e) {
			PluginManager.log("WARNING: BuildConfig class not found!");
			PluginManager.log("Possible cause: The gradle buildConfig task was not executed for some reason.");
			PluginManager.log("Using fallback values instead of actual");
			info.put("NAME", "DeskChan");
		}

		locales=new HashMap<>();
		locales.put("ru","Русский");
		locales.put("en","English");
		boolean foundDef=false;
		for(Map.Entry<String,String> locale : locales.entrySet()){
			if(locale.getValue().equals(Locale.getDefault().getLanguage())){
				foundDef=true;
				break;
			}
		}
		if(!foundDef) Locale.setDefault(new Locale("en"));
	}
	
}
