package info.deskchan.core;

public interface Plugin {
	
	default boolean initialize(PluginProxyInterface proxy) {
		return true;
	}
	
	default void unload() { }
	
}
