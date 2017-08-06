package info.deskchan.core;

import java.nio.file.Path;
import java.util.*;

public class CorePlugin implements Plugin, MessageListener {
	
	private PluginProxyInterface pluginProxy = null;
	private final Map<String, List<AlternativeInfo>> alternatives = new HashMap<>();
	private final Map<String, PipeInfo> pipes = new HashMap<>();
	class QuitTimerTask extends TimerTask {
		@Override
		public void run() {
			System.exit(0);
		}
	}
	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		this.pluginProxy = pluginProxy;
		pluginProxy.addMessageListener("core:quit", (sender, tag, data) -> {
			Long delay=0L;
			if(data!=null){
				if(data instanceof Integer) delay=((Integer) data).longValue();
				else if(data instanceof Long) delay=(Long) data;
			}
			if(delay>0) {
				Timer timer = new Timer();
				pluginProxy.log("Plugin " + sender + " requested application quit with delay: " + delay);
				PluginManager.getInstance().unloadPlugins();
				timer.schedule(new QuitTimerTask(), delay);
			} else {
				pluginProxy.log("Plugin " + sender + " requested application quit");
				PluginManager.getInstance().unloadPlugins();
				(new QuitTimerTask()).run();
			}
		});
		pluginProxy.addMessageListener("core:register-alternative", (sender, tag, data) -> {
			Map m = (Map) data;
			registerAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(),
					sender, (Integer) m.get("priority"));
		});
		pluginProxy.addMessageListener("core:register-alternatives", (sender, tag, data) -> {
			List<Map> alternativeList = (List<Map>) data;
			for (Map m : alternativeList) {
				registerAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(),
						sender, (Integer) m.get("priority"));
			}
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
				put("seq", seq);
				put("map", getAlternativesMap());
			}});
		});
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			if(data==null) {
				PluginManager.log("attempt to unload null plugin");
				return;
			}
			String plugin = data.toString();
			synchronized (this) {
				Iterator<Map.Entry<String, List<AlternativeInfo>>> mapIterator = alternatives.entrySet().iterator();
				while (mapIterator.hasNext()) {
					Map.Entry<String, List<AlternativeInfo>> entry = mapIterator.next();
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
			synchronized (this) {
				Iterator<Map.Entry<String, PipeInfo>> mapIterator = pipes.entrySet().iterator();
				while (mapIterator.hasNext()) {
					Map.Entry<String, PipeInfo> entry = mapIterator.next();
					PipeInfo pipeInfo = entry.getValue();
					if (pipeInfo.plugin.equals(plugin)) {
						pluginProxy.removeMessageListener(entry.getKey(), pipeInfo.messageListener);
						pluginProxy.log("Destroyed pipe " + entry.getKey());
						mapIterator.remove();
						continue;
					}
					pipeInfo.remove(plugin);
				}
			}
		});
		pluginProxy.addMessageListener("core:get-plugin-data-dir", (sender, tag, data) -> {
			Path pluginDataDirPath = PluginManager.getPluginDataDirPath(sender);
			Object seq = ((Map) data).get("seq");
			pluginProxy.sendMessage(sender, new HashMap<String, Object>() {{
				put("seq", seq);
				put("path", pluginDataDirPath.toString());
			}});
		});
		pluginProxy.addMessageListener("core:create-pipe", (sender, tag, data) -> {
			String name = data.toString();
			if (!pipes.containsKey(name)) {
				PipeInfo pipeInfo = new PipeInfo(name, sender);
				pipes.put(name, pipeInfo);
				pluginProxy.log("Created pipe " + name);
				pluginProxy.addMessageListener(name, pipeInfo.messageListener);
			}
		});
		pluginProxy.addMessageListener("core:destroy-pipe", (sender, tag, data) -> {
			String name = data.toString();
			PipeInfo pipeInfo = pipes.getOrDefault(name, null);
			if (pipeInfo != null) {
				if (pipeInfo.plugin.equals(sender)) {
					pipes.remove(name);
					pluginProxy.removeMessageListener(name, pipeInfo.messageListener);
					pluginProxy.log("Destroyed pipe " + name);
				}
			}
		});
		pluginProxy.addMessageListener("core:append-pipe-stage", (sender, tag, data) -> {
			Map<String, Object> m = (Map<String, Object>) data;
			String pipeName = (String) m.get("pipe");
			String stageTag = (String) m.get("tag");
			synchronized (this) {
				PipeInfo pipeInfo = pipes.getOrDefault(pipeName, null);
				if (pipeInfo != null) {
					pipeInfo.append(sender, stageTag);
				}
			}
		});
		pluginProxy.addMessageListener("core:prepend-pipe-stage", (sender, tag, data) -> {
			Map<String, Object> m = (Map<String, Object>) data;
			String pipeName = (String) m.get("pipe");
			String stageTag = (String) m.get("tag");
			synchronized (this) {
				PipeInfo pipeInfo = pipes.getOrDefault(pipeName, null);
				if (pipeInfo != null) {
					pipeInfo.prepend(sender, stageTag);
				}
			}
		});
		pluginProxy.addMessageListener("core:remove-pipe-stage", (sender, tag, data) -> {
			Map<String, Object> m = (Map<String, Object>) data;
			String pipeName = (String) m.get("pipe");
			String stageTag = (String) m.get("tag");
			synchronized (this) {
				PipeInfo pipeInfo = pipes.getOrDefault(pipeName, null);
				if (pipeInfo != null) {
					pipeInfo.remove(sender, stageTag);
				}
			}
		});
		CommandsProxy.initialize(pluginProxy);
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
			if (info.priority < priority) {
				iterator.previous();
				break;
			}
		}
		iterator.add(new AlternativeInfo(dstTag, plugin, priority));
		if (l.size() == 1) {
			pluginProxy.addMessageListener(srcTag, this);
		}
		pluginProxy.log("Registered alternative " + dstTag + " for tag " + srcTag + " by plugin " + plugin);
	}
	
	private void unregisterAlternative(String srcTag, String dstTag, String plugin) {
		List<AlternativeInfo> l = alternatives.getOrDefault(srcTag, null);
		if (l == null) {
			return;
		}
		if (l.removeIf(info -> info.plugin.equals(plugin) && info.tag.equals(dstTag))) {
			pluginProxy.log("Unregistered alternative " + dstTag + " for tag " + srcTag);
		}
		if (l.isEmpty()) {
			alternatives.remove(srcTag);
			pluginProxy.removeMessageListener(srcTag, this);
			pluginProxy.log("No more alternatives for " + srcTag);
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

			if (!info.tag.equals(dstTag)) continue;
			if (info.priority == priority) break;

			iterator.remove();
			iterator = l.listIterator();
			while (iterator.hasNext()) {
				AlternativeInfo info2 = iterator.next();
				if (info2.priority < priority) {
					iterator.previous();
					break;
				}
			}
			info.priority = priority;
			iterator.add(info);
			break;
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
		if (l == null) {
			return;
		}
		if (l.isEmpty()) {
			return;
		}
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
	
	private class PipeInfo {
		String name;
		final String plugin;
		final List<PipeStageInfo> stages = new ArrayList<>();
		MessageListener messageListener = (sender, tag, data) -> {
			if (!(data instanceof Map)) {
				Map<String, Object> m = new HashMap<>();
				m.put("data", data);
				data = m;
			}
			Map<String, Object> m = (Map<String, Object>) data;
			Object seq = m.getOrDefault("seq", null);
			PipeState state;
			if (seq instanceof PipeState) {
				state = (PipeState) seq;
			} else {
				state = new PipeState(sender, seq);
				m.put("seq", state);
			}
			synchronized (stages) {
				if (state.offset >= stages.size()) {
					m.put("seq", state.seq);
					PluginManager.getInstance().sendMessage(name, state.replyTo, m);
				} else {
					state.offset++;
					PluginManager.getInstance().sendMessage(name, stages.get(state.offset).tag, m);
				}
			}
		};
		
		PipeInfo(String name, String plugin) {
			this.name = name;
			this.plugin = plugin;
		}
		
		void append(String plugin, String tag) {
			synchronized (stages) {
				stages.add(new PipeStageInfo(plugin, tag));
			}
		}
		
		void prepend(String plugin, String tag) {
			synchronized (stages) {
				stages.add(0, new PipeStageInfo(plugin, tag));
			}
		}
		
		void remove(String plugin, String tag) {
			synchronized (stages) {
				stages.removeIf(stageInfo -> stageInfo.plugin.equals(plugin) && stageInfo.tag.equals(tag));
			}
		}
		
		void remove(String plugin) {
			synchronized (stages) {
				stages.removeIf(stageInfo -> stageInfo.plugin.equals(plugin));
			}
		}
	}
	
	private static class PipeStageInfo {
		final String plugin;
		final String tag;
		
		PipeStageInfo(String plugin, String tag) {
			this.plugin = plugin;
			this.tag = tag;
		}
	}
	
	private static class PipeState {
		int offset = 0;
		final String replyTo;
		final Object seq;
		
		PipeState(String replyTo, Object seq) {
			this.replyTo = replyTo;
			this.seq = seq;
		}
	}
	
}
