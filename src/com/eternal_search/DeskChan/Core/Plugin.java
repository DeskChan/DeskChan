package com.eternal_search.DeskChan.Core;

public interface Plugin {
	
	default boolean initialize(PluginProxy proxy) {
		return true;
	}
	
	default void unload() {
	}
	
}
