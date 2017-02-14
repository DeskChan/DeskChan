package com.eternal_search.deskchan.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PluginProxy {
	
	private final Plugin plugin;
	private String id = null;
	private Map<String, Set<MessageListener>> messageListeners = new HashMap<>();
	
	PluginProxy(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean initialize(String id) {
		assert this.id == null;
		this.id = id;
		return plugin.initialize(this);
	}
	
	public void unload() {
		assert id != null;
		plugin.unload();
		for (Map.Entry<String, Set<MessageListener>> entry: messageListeners.entrySet()) {
			for (MessageListener listener : entry.getValue()) {
				PluginManager.getInstance().unregisterMessageListener(entry.getKey(), listener);
			}
		}
		messageListeners.clear();
		PluginManager.getInstance().unregisterPlugin(this);
		id = null;
	}
	
	public void sendMessage(String tag, Object data) {
		PluginManager.getInstance().sendMessage(id, tag, data);
	}
	
	public void addMessageListener(String tag, MessageListener listener) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners == null) {
			listeners = new HashSet<>();
			messageListeners.put(tag, listeners);
		}
		listeners.add(listener);
		PluginManager.getInstance().registerMessageListener(tag, listener);
	}
	
	public void removeMessageListener(String tag, MessageListener listener) {
		PluginManager.getInstance().unregisterMessageListener(tag, listener);
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.size() == 0) {
				messageListeners.remove(tag);
			}
		}
	}
	
}
