package info.deskchan.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface MessageData {
	@Retention(RetentionPolicy.RUNTIME)
	@interface FieldName {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface Ignore {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Tag {
		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface RequiresResponse {
	}

}
