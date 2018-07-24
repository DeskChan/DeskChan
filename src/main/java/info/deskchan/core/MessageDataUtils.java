package info.deskchan.core;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
		if (m == null) {
			return null;
		}
		Object originalInstance = m.get(ORIGINAL_INSTANCE_KEY);
		if ((originalInstance != null) && cls.isAssignableFrom(originalInstance.getClass())) {
			return (T) originalInstance;
		}
		try {
			Constructor<?>[] constructors = cls.getDeclaredConstructors();
			for (Constructor<?> constructor : constructors) {
				if (!Modifier.isPublic(constructor.getModifiers())) {
					continue;
				}
				if (constructor.getParameterCount() == 0) {
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
				}
			}
			if (constructors.length != 1) {
				throw new IllegalArgumentException("Class must have only one constructor");
			}
			Constructor<?> constructor = constructors[0];
			List<Object> arguments = new ArrayList<>();
			for (Parameter parameter : constructor.getParameters()) {
				if (parameter.isNamePresent()) {
					Object value = m.get(parameter.getName());
					Class<?> parameterType = parameter.getType();
					if (parameterType.isAnnotationPresent(MessageData.class)) {
						value = deserialize((Map<String, Object>) value, parameterType);
					}
					arguments.add(value);
				} else {
					arguments.add(null);
				}
			}
			return (T) constructor.newInstance(arguments.toArray(new Object[arguments.size()]));
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			return null;
		}
	}
}
