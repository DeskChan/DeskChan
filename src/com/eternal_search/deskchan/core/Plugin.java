package com.eternal_search.deskchan.core;

public interface Plugin {
	
	default boolean initialize(PluginProxy proxy) {
		return true;
	}
	
	default void unload() {
	}
	
}
