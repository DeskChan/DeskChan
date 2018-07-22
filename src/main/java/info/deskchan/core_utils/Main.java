package info.deskchan.core_utils;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core.ResponseListener;

import java.util.*;

public class Main implements Plugin {

	private static PluginProxyInterface pluginProxy;
	private final List<DelayNotifier> timerTasks = new LinkedList<>();
	private final Timer timer = new Timer();

	@Override
	public boolean initialize(PluginProxyInterface proxy) {
		pluginProxy = proxy;

		pluginProxy.setConfigField("name", pluginProxy.getString("core-utils-plugin-name"));

		pluginProxy.addMessageListener("core-utils:notify-after-delay-default-impl",
				(sender, tag, data) -> {
					Map m = (Map) data;

					// canceling timer
					Integer seq = (Integer) m.get("cancel");
					if (seq != null) {
						synchronized (timerTasks) {
							String cancelTag = sender + "#" + seq;
							Iterator<DelayNotifier> iterator = timerTasks.iterator();
							while (iterator.hasNext()) {
								DelayNotifier task = iterator.next();
								if (task.tag.equals(cancelTag)) {
									task.cancel();
									iterator.remove();
								}
							}
							return;
						}
					}

					Object delayObj = m.getOrDefault("delay", -1L);
					long delay = 1000;
					if(delayObj instanceof Number)
						delay = ((Number) delayObj).longValue();
					else
						delay = Long.valueOf(delayObj.toString());

					if (delay > 0) {
						DelayNotifier task = new DelayNotifier(sender);
						timer.schedule(task, delay);
					}
		});

		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			synchronized (timerTasks) {
				Iterator<DelayNotifier> iterator = timerTasks.iterator();
				while (iterator.hasNext()) {
					DelayNotifier task = iterator.next();
					if (task.tag.startsWith(data.toString())) {
						task.cancel();
						iterator.remove();
					}
				}
			}
		});

		pluginProxy.addMessageListener("core:distribute-resources", (sender, tag, data) -> {
			ResourceDistributor.distribute((String) data);
		});

		pluginProxy.setAlternative("core-utils:notify-after-delay", "core-utils:notify-after-delay-default-impl", 1);

		pluginProxy.addMessageListener("core:open-link", (sender, tag, data) -> {
			if (data == null) return;

			String value;
			if (data instanceof Map)
				value = (String) ((Map) data).get("value");
			else
				value = data.toString();

			try {
				Browser.browse(value);
			} catch (Exception e){
				pluginProxy.log(e);
			}
		});

		UserSpeechRequest.initialize(pluginProxy);

		pluginProxy.getProperties().load();
		if (pluginProxy.getProperties().getBoolean("terminal", false))
			TerminalGUI.initialize();

		pluginProxy.setTimer(20000, -1, new ResponseListener() {
			@Override
			public void handle(String sender, Object data) {
				System.gc();
			}
		});
		return true;
	}


	@Override
	public void unload() {
		timer.purge();
	}
	
	class DelayNotifier extends TimerTask {
		
		private final String tag;
		
		DelayNotifier(String tag) {
			this.tag = tag;
			synchronized (timerTasks) {
				timerTasks.add(this);
			}
		}
		
		@Override
		public void run() {
			synchronized (timerTasks) {
				timerTasks.remove(this);
			}
			pluginProxy.sendMessage(tag, null);
		}
	}

	static void log(String text) {
		pluginProxy.log(text);
	}

	static void log(Throwable e) {
		pluginProxy.log(e);
	}

	static PluginProxyInterface getPluginProxy() { return pluginProxy; }

}
