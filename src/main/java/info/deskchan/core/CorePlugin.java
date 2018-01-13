package info.deskchan.core;

import java.nio.file.Path;
import java.util.*;

public class CorePlugin implements Plugin, MessageListener {
	
	protected PluginProxyInterface pluginProxy = null;
	protected final Map<String, List<AlternativeInfo>> alternatives = new HashMap<>();

	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		this.pluginProxy = pluginProxy;

		/* Quit program.
		* Public message
        * Params: delay: Long? - delay in ms program will quit after, default - 0
        * Returns: None */
		pluginProxy.addMessageListener("core:quit", (sender, tag, data) -> {
			int delay = 0;
			if (data != null) {
				if (data instanceof Map) {
					delay = (int) ((Map<String, Object>) data).getOrDefault("delay", 0);
				} else if (data instanceof Number) {
					delay = ((Number) data).intValue();
				}
			}
			Map<String, Object> m = new HashMap<>();
			m.put("delay", delay);
			pluginProxy.log("Plugin " + sender + " requested application quit in " + delay / 1000 + " seconds.");
			if(delay > 20)
				pluginProxy.sendMessage("core-utils:notify-after-delay", m, (s, d) -> PluginManager.getInstance().quit() );
			else
			{
				PluginManager.getInstance().quit();
				System.exit(0);
			}

		});

		/* Registers alternative to tag. All messages sent to srcTag will be redirected to dstTag
		   if dstTag priority is max.
		* Public message
        * Params: srcTag: String! - source tag to redirect
        *         dstTag: String! - destination tag
        *         priority: String! - priority of alternative
        * Returns: None */
		pluginProxy.addMessageListener("core:register-alternative", (sender, tag, data) -> {
			Map m = (Map) data;
			registerAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(),
					sender, m.get("priority"));
		});

		/* Registers alternatives. Look for core:register-alternative
		* Public message
        * Params: List of Map
        * 		    srcTag: String! - source tag to redirect
        *           dstTag: String! - destination tag
        *           priority: String! - priority of alternative
        * Returns: None */
		pluginProxy.addMessageListener("core:register-alternatives", (sender, tag, data) -> {
			List<Map> alternativeList = (List<Map>) data;
			for (Map m : alternativeList) {
				registerAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(),
						sender, m.get("priority"));
			}
		});

		/* Unregisters alternative. Look for core:register-alternative
		* Public message
        * Params: srcTag: String! - source tag to redirect
        *         dstTag: String! - destination tag
        * Returns: None */
		pluginProxy.addMessageListener("core:unregister-alternative", (sender, tag, data) -> {
			Map m = (Map) data;
			unregisterAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(), sender);
		});

		/* Get alternatives map.
		* Public message
        * Params: None
        * Returns: Map of Lists of Maps, "source" -> "alternatives", every list descending by priority
        *            tag: String - destination tag
        *            plugin: String - owner of destination tag
        *            priority: Int - priority of alternative*/
		pluginProxy.addMessageListener("core:query-alternatives-map", (sender, tag, data) -> {
			pluginProxy.sendMessage(sender, getAlternativesMap());
		});

		/* Clearing all dependencies of unloaded plugin.
		 * Technical message
		 * Params: name: String - name of plugin
		 * Returns: None  */
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			if(data == null) {
				PluginManager.log("attempt to unload null plugin");
				return;
			}
			String plugin = data.toString();
			synchronized (this) {
				Iterator < Map.Entry<String, List<AlternativeInfo>> > mapIterator = alternatives.entrySet().iterator();
				while (mapIterator.hasNext()) {
					Map.Entry <String, List<AlternativeInfo>> entry = mapIterator.next();
					List<AlternativeInfo> l = entry.getValue();
					Iterator<AlternativeInfo> iterator = l.iterator();
					while (iterator.hasNext()) {
						AlternativeInfo info = iterator.next();
						if (info.plugin.equals(plugin)) {
							iterator.remove();
							pluginProxy.log("Unregistered alternative " + info.tag + " for tag " + entry.getKey());
						}
					}
					if (l.isEmpty()) {
						String srcTag = entry.getKey();
						pluginProxy.removeMessageListener(srcTag, this);
						pluginProxy.log("No more alternatives for " + srcTag);
						mapIterator.remove();
					}
				}
			}
		});

		/* Get plugin data directory.
		 * Public message
		 * Params: None
		 * Returns: String - path to directory  */
		pluginProxy.addMessageListener("core:get-plugin-data-dir", (sender, tag, data) -> {
			Path pluginDataDirPath = PluginManager.getPluginDataDirPath(sender);
			pluginProxy.sendMessage(sender, pluginDataDirPath.toString());
		});

		CommandsProxy.initialize(pluginProxy);

		return true;
	}

	private void registerAlternative(String srcTag, String dstTag, String plugin, Object priority) {
		int _priority;
		if (priority instanceof Number)
			_priority = ((Number) priority).intValue();
		else {
			try {
				_priority = Integer.parseInt(priority.toString());
			} catch (Exception e){
				throw new ClassCastException(
						"Cannot cast '" + priority.toString() + "', type=" + priority.getClass() + "  to integer");
			}
		}

		List<AlternativeInfo> list = alternatives.get(srcTag);
		if (list == null) {
			list = new LinkedList<>();
			alternatives.put(srcTag, list);
			pluginProxy.addMessageListener(srcTag, this);
		}

		Iterator<AlternativeInfo> iterator = list.iterator();
		while (iterator.hasNext()) {
			AlternativeInfo info = iterator.next();
			if (info.isEqual(dstTag, plugin)) {
				iterator.remove();
				break;
			}
		}

		list.add(new AlternativeInfo(dstTag, plugin, _priority));
		list.sort(new Comparator<AlternativeInfo>() {
			@Override
			public int compare(AlternativeInfo o1, AlternativeInfo o2) {
				return Integer.compare(o2.priority, o1.priority);
			}
		});

		pluginProxy.log("Registered alternative " + dstTag + " for tag " + srcTag + " with priority: " + priority + ", by plugin " + plugin);
	}
	
	private void unregisterAlternative(String srcTag, String dstTag, String plugin) {
		List<AlternativeInfo> list = alternatives.get(srcTag);
		if (list == null)
			return;

		Iterator<AlternativeInfo> iterator = list.iterator();
		while (iterator.hasNext()) {
			AlternativeInfo info = iterator.next();
			if (info.isEqual(dstTag, plugin)) {
				iterator.remove();
				pluginProxy.log("Unregistered alternative " + dstTag + " for tag " + srcTag);
				break;
			}
		}

		if (list.isEmpty()) {
			alternatives.remove(srcTag);
			pluginProxy.removeMessageListener(srcTag, this);
			pluginProxy.log("No more alternatives for " + srcTag);
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
		int delimiter = tag.indexOf("#");
		String senderTag = null;
		if (delimiter > 0) {
			senderTag = tag.substring(delimiter + 1);
			tag = tag.substring(0, delimiter);
		}

		List<AlternativeInfo> list = alternatives.get(tag);
		if (list == null || list.isEmpty())
			return;

		Iterator<AlternativeInfo> iterator = list.iterator();
		if (senderTag != null){
			do {
				AlternativeInfo nextInfo = iterator.next();
				if (nextInfo.tag.equals(senderTag)) break;
			} while (true);
		}

		try {
			AlternativeInfo info = iterator.next();
			PluginManager.getInstance().sendMessage(sender, info.tag, data);
		} catch (NoSuchElementException e) { }
	}
	
	static class AlternativeInfo {
		String tag;
		String plugin;
		int priority;
		
		AlternativeInfo(String tag, String plugin, int priority) {
			this.tag = tag;
			this.plugin = plugin;
			this.priority = priority;
		}

		boolean isEqual(String tag, String plugin){
			return this.tag.equals(tag) && this.plugin.equals(plugin);
		}

		@Override
		public String toString(){
			return tag + "(" + plugin + ")" + "=" + priority;
		}
	}
}
