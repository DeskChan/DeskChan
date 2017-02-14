package com.eternal_search.DeskChan.Core;

public interface MessageListener {
	
	void handleMessage(String sender, String tag, Object data);
	
}
