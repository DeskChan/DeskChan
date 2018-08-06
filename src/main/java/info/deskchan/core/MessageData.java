package info.deskchan.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MessageData {
	@Retention(RetentionPolicy.RUNTIME)
	@interface FieldName {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface Ignore {
	}
}
