package info.deskchan.core;

import java.lang.reflect.Field;
import java.util.HashMap;
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
	
	public static void PrintInfo(){
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
			PluginManager.log(CoreInfo.get("NAME") + " " + CoreInfo.get("VERSION"));
			PluginManager.log("Go to " + CoreInfo.get("PROJECT_SITE_URL") + " for more information");
			PluginManager.log("Git branch: " + CoreInfo.get("GIT_BRANCH_NAME"));
			PluginManager.log("Git commit hash: " + CoreInfo.get("GIT_COMMIT_HASH"));
			PluginManager.log("Build date and time: " + CoreInfo.get("BUILD_DATETIME"));
		} catch (ClassNotFoundException e) {
			PluginManager.log("DeskChan: unknown source, unknown version.");
			PluginManager.log("No additional info because you are building project without gradle support.");
		}
	}
	
}
