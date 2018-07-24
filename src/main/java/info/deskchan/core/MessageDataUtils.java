package info.deskchan.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public final class MessageDataUtils {
	private static final String ORIGINAL_INSTANCE_KEY = "__original_object_instance__";
	
	private MessageDataUtils() {
	}
	
	public static Object serialize(Object data) {
		if (data == null) {
			return null;
		}
		if (!data.getClass().isAnnotationPresent(MessageData.class)) {
			return data;
		}
		Map<String, Object> m = new HashMap<>();
		for (Field field : data.getClass().getDeclaredFields()) {
			if (!Modifier.isPublic(field.getModifiers())) {
				continue;
			}
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (field.isAnnotationPresent(MessageData.Ignore.class)) {
				continue;
			}
			MessageData.FieldName fieldNameAnnotation = field.getAnnotation(MessageData.FieldName.class);
			String fieldName = fieldNameAnnotation != null ? fieldNameAnnotation.value() : field.getName();
			try {
				m.put(fieldName, serialize(field.get(data)));
			} catch (IllegalAccessException e) {
				// Do nothing
			}
		}
		m.put(ORIGINAL_INSTANCE_KEY, data);
		return m;
	}
	
	public static <T> T deserialize(Map<String, Object> m, Class<T> cls) {
		Object originalInstance = m.get(ORIGINAL_INSTANCE_KEY);
		if ((originalInstance != null) && cls.isAssignableFrom(originalInstance.getClass())) {
			return (T) originalInstance;
		}
		try {
			T object = cls.newInstance();
			for (Field field : cls.getDeclaredFields()) {
				if (!Modifier.isPublic(field.getModifiers())) {
					continue;
				}
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (field.isAnnotationPresent(MessageData.Ignore.class)) {
					continue;
				}
				MessageData.FieldName fieldNameAnnotation = field.getAnnotation(MessageData.FieldName.class);
				String fieldName = fieldNameAnnotation != null ? fieldNameAnnotation.value() : field.getName();
				if (m.containsKey(fieldName)) {
					try {
						Class<?> fieldCls = field.getType();
						Object value = m.get(fieldName);
						if (fieldCls.isAnnotationPresent(MessageData.class)) {
							value = deserialize((Map<String, Object>) value, cls);
						}
						field.set(object, value);
					} catch (IllegalAccessException e) {
						// Do nothing
					}
				}
			}
			return object;
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
}
