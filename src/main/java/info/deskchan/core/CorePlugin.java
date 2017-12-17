package info.deskchan.core;

import java.nio.file.Path;
import java.util.*;

public class CorePlugin implements Plugin, MessageListener {
	
	private PluginProxyInterface pluginProxy = null;
	private final Map<String, Queue<AlternativeInfo>> alternatives = new HashMap<>();

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
				PluginManager.getInstance().quit();
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
				Iterator < Map.Entry<String, Queue<AlternativeInfo>> > mapIterator = alternatives.entrySet().iterator();
				while (mapIterator.hasNext()) {
					Map.Entry <String, Queue<AlternativeInfo>> entry = mapIterator.next();
					Queue<AlternativeInfo> l = entry.getValue();
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
		// Testing();

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

		Queue<AlternativeInfo> queue = alternatives.get(srcTag);
		if (queue == null) {
			queue = new PriorityQueue<>(new Comparator<AlternativeInfo>() {
				@Override
				public int compare(AlternativeInfo o1, AlternativeInfo o2) {
					return Integer.compare(o2.priority, o1.priority);
				}
			});
			alternatives.put(srcTag, queue);
			pluginProxy.addMessageListener(srcTag, this);
		}

		Iterator<AlternativeInfo> iterator = queue.iterator();
		while (iterator.hasNext()) {
			AlternativeInfo info = iterator.next();
			if (info.isEqual(dstTag, plugin)) {
				iterator.remove();
				break;
			}
		}

		queue.add(new AlternativeInfo(dstTag, plugin, _priority));

		pluginProxy.log("Registered alternative " + dstTag + " for tag " + srcTag + " with priority: " + priority + ", by plugin " + plugin);
	}
	
	private void unregisterAlternative(String srcTag, String dstTag, String plugin) {
		Queue<AlternativeInfo> queue = alternatives.get(srcTag);
		if (queue == null)
			return;

		Iterator<AlternativeInfo> iterator = queue.iterator();
		while (iterator.hasNext()) {
			AlternativeInfo info = iterator.next();
			if (info.isEqual(dstTag, plugin)) {
				iterator.remove();
				pluginProxy.log("Unregistered alternative " + dstTag + " for tag " + srcTag);
				break;
			}
		}

		if (queue.isEmpty()) {
			alternatives.remove(srcTag);
			pluginProxy.removeMessageListener(srcTag, this);
			pluginProxy.log("No more alternatives for " + srcTag);
		}
	}

	private Map<String, Object> getAlternativesMap() {
		Map<String, Object> m = new HashMap<>();
		for (Map.Entry<String, Queue<AlternativeInfo>> entry : alternatives.entrySet()) {
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

		Queue<AlternativeInfo> queue = alternatives.get(tag);
		if (queue == null || queue.isEmpty())
			return;

		Iterator<AlternativeInfo> iterator = queue.iterator();
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
	
	private static class AlternativeInfo {
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

	/** Testing alternatives. Very good example of using. **/
	void Testing(){
		pluginProxy.addMessageListener("core:test1", (sender, tag, data) -> {
			System.out.println("test 1");
			pluginProxy.sendMessage("DeskChan:test#core:test1", null);
		});

		pluginProxy.addMessageListener("core:test2", (sender, tag, data) -> {
			System.out.println("test 2");
			pluginProxy.sendMessage("DeskChan:test#core:test2", null);
		});

		pluginProxy.addMessageListener("core:test3", (sender, tag, data) -> {
			System.out.println("test 3");
		});

		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>(){{
			put("srcTag", "DeskChan:test");
			put("dstTag", "core:test1");
			put("priority", 1000);
		}});

		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>(){{
			put("srcTag", "DeskChan:test");
			put("dstTag", "core:test2");
			put("priority", 500);
		}});

		System.out.println(alternatives);
		pluginProxy.sendMessage("DeskChan:test", null);

		pluginProxy.sendMessage("core:unregister-alternative", new HashMap<String, Object>(){{
			put("srcTag", "DeskChan:test");
			put("dstTag", "core:test1");
		}});

		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>(){{
			put("srcTag", "DeskChan:test");
			put("dstTag", "core:test3");
			put("priority", 1500);
		}});

		System.out.println(alternatives);
		pluginProxy.sendMessage("DeskChan:test", null);

		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>(){{
			put("srcTag", "DeskChan:test");
			put("dstTag", "core:test3");
			put("priority", 400);
		}});

		System.out.println(alternatives);
		pluginProxy.sendMessage("DeskChan:test", null);

	}
}
