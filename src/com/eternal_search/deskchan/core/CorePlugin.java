package com.eternal_search.deskchan.core;

import java.util.*;

public class CorePlugin implements Plugin, MessageListener {
	
	private PluginProxy pluginProxy = null;
	private final Map<String, List<AlternativeInfo>> alternatives = new HashMap<>();
	
	@Override
	public boolean initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		pluginProxy.addMessageListener("core:quit", (sender, tag, data) -> {
			System.err.println("Plugin " + sender + " requested application quit");
			PluginManager.getInstance().quit();
		});
		pluginProxy.addMessageListener("core:register-alternative", (sender, tag, data) -> {
			Map m = (Map) data;
			registerAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(),
					sender, (Integer) m.get("priority"));
		});
		pluginProxy.addMessageListener("core:unregister-alternative", (sender, tag, data) -> {
			Map m = (Map) data;
			unregisterAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(), sender);
		});
		pluginProxy.addMessageListener("core:change-alternative-priority", (sender, tag, data) -> {
			Map m = (Map) data;
			changeAlternativePriority(m.get("srcTag").toString(), m.get("dstTag").toString(),
					(Integer) m.get("priority"));
		});
		pluginProxy.addMessageListener("core:query-alternatives-map", (sender, tag, data) -> {
			Object seq = ((Map) data).get("seq");
			pluginProxy.sendMessage(sender, new HashMap<String, Object>() {{
				put("seq", seq); put("map", getAlternativesMap());
			}});
		});
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			String plugin = data.toString();
			Iterator<Map.Entry<String, List<AlternativeInfo>>> mapIterator = alternatives.entrySet().iterator();
			while (mapIterator.hasNext()) {
				Map.Entry<String, List<AlternativeInfo>> entry = mapIterator.next();
				List<AlternativeInfo> l = entry.getValue();
				Iterator<AlternativeInfo> iterator = l.iterator();
				while (iterator.hasNext()) {
					AlternativeInfo info = iterator.next();
					if (info.plugin.equals(plugin)) {
						iterator.remove();
						System.err.println("Unregistered alternative " + info.tag + " for tag " + entry.getKey());
					}
				}
				if (l.isEmpty()) {
					String srcTag = entry.getKey();
					pluginProxy.removeMessageListener(srcTag, this);
					System.err.println("No more alternatives for " + srcTag);
					mapIterator.remove();
				}
			}
		});
		return true;
	}
	
	private void registerAlternative(String srcTag, String dstTag, String plugin, int priority) {
		List<AlternativeInfo> l = alternatives.getOrDefault(srcTag, null);
		if (l == null) {
			l = new ArrayList<>();
			alternatives.put(srcTag, l);
		}
		ListIterator<AlternativeInfo> iterator = l.listIterator();
		while (iterator.hasNext()) {
			AlternativeInfo info = iterator.next();
			if (info.plugin.equals(plugin) && info.tag.equals(dstTag)) {
				changeAlternativePriority(srcTag, dstTag, priority);
				return;
			}
			if (info.priority < priority) break;
		}
		iterator.add(new AlternativeInfo(dstTag, plugin, priority));
		if (l.size() == 1) {
			pluginProxy.addMessageListener(srcTag, this);
		}
		System.err.println("Registered alternative " + dstTag + " for tag " + srcTag + " by plugin " + plugin);
	}
	
	private void unregisterAlternative(String srcTag, String dstTag, String plugin) {
		List<AlternativeInfo> l = alternatives.getOrDefault(srcTag, null);
		if (l == null) {
			return;
		}
		if (l.removeIf(info -> info.plugin.equals(plugin) && info.tag.equals(dstTag))) {
			System.err.println("Unregistered alternative " + dstTag + " for tag " + srcTag);
		}
		if (l.isEmpty()) {
			alternatives.remove(srcTag);
			pluginProxy.removeMessageListener(srcTag, this);
			System.err.println("No more alternatives for " + srcTag);
		}
	}
	
	private void changeAlternativePriority(String srcTag, String dstTag, int priority) {
		List<AlternativeInfo> l = alternatives.getOrDefault(srcTag, null);
		if (l == null) {
			return;
		}
		ListIterator<AlternativeInfo> iterator = l.listIterator();
		while (iterator.hasNext()) {
			AlternativeInfo info = iterator.next();
			if (info.tag.equals(dstTag) && (info.priority != priority)) {
				iterator.remove();
				iterator = l.listIterator();
				while (iterator.hasNext()) {
					AlternativeInfo info2 = iterator.next();
					if (info2.priority < priority) break;
				}
				info.priority = priority;
				iterator.add(info);
				break;
			}
		}
	}
	
	private Map<String, Object> getAlternativesMap() {
		Map<String, Object> m = new HashMap<>();
		for (Map.Entry<String, List<AlternativeInfo>> entry : alternatives.entrySet()) {
			List<Map<String, Object>> l = new ArrayList<>();
			for (AlternativeInfo info : entry.getValue()) {
				l.add(new HashMap<String, Object>() {{
					put("tag", info.tag);
					put("plugin", info.plugin);
					put("priority", info.priority);
				}});
			}
			m.put(entry.getKey(), l);
		}
		return m;
	}
	
	@Override
	public void handleMessage(String sender, String tag, Object data) {
		List<AlternativeInfo> l = alternatives.getOrDefault(tag, null);
		if (l == null) return;
		if (l.isEmpty()) return;
		AlternativeInfo info = l.get(0);
		PluginManager.getInstance().sendMessage(sender, info.tag, data);
	}
	
	private static class AlternativeInfo {
		String tag;
		String plugin;
		int priority;
		
		AlternativeInfo(String tag, String plugin, int priority) {
			this.tag = tag;
			this.plugin = plugin;
			this.priority = priority;
		}
		
	}
	
}
