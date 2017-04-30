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
			PluginManager.log(e);
		}
	}
	
}
