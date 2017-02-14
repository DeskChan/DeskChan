package com.eternal_search.deskchan.core;

public interface MessageListener {
	
	void handleMessage(String sender, String tag, Object data);
	
}
