package info.deskchan.core_utils;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;

import java.util.*;

public class Main implements Plugin {

	private static PluginProxyInterface pluginProxy;
	private final List<MyTimerTask> timerTasks = new LinkedList<>();
	private final Timer timer = new Timer();
	
	@Override
	public boolean initialize(PluginProxyInterface proxy) {
		pluginProxy = proxy;
		pluginProxy.addMessageListener("core-utils:notify-after-delay-default-impl",
				(sender, tag, data) -> {
					Map m = (Map) data;
					Object seq = m.get("seq");
					synchronized (timerTasks) {
						Iterator<MyTimerTask> iterator = timerTasks.iterator();
						while (iterator.hasNext()) {
							MyTimerTask task = iterator.next();
							if (task.plugin.equals(sender) && task.seq.equals(seq)) {
								task.cancel();
								iterator.remove();
							}
						}
					}
					Object delayObj = m.getOrDefault("delay", -1L);
					long delay = delayObj instanceof Integer ? (long) (int) delayObj : (long) delayObj;
					if (delay > 0) {
						MyTimerTask task = new MyTimerTask(sender, seq);
						timer.schedule(task, delay);
					}
				});
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			synchronized (timerTasks) {
				Iterator<MyTimerTask> iterator = timerTasks.iterator();
				while (iterator.hasNext()) {
					MyTimerTask task = iterator.next();
					if (task.plugin.equals(data)) {
						task.cancel();
						iterator.remove();
					}
				}
			}
		});
		pluginProxy.addMessageListener("core:distribute-resources", (sender, tag, data) -> {
			ResourceDistributor.distribute((String) data);
		});
		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
			put("srcTag", "core-utils:notify-after-delay");
			put("dstTag", "core-utils:notify-after-delay-default-impl");
			put("priority", 1);
		}});
		return true;
	}
	
	@Override
	public void unload() {
		timer.purge();
	}
	
	class MyTimerTask extends TimerTask {
		
		private final String plugin;
		private final Object seq;
		
		MyTimerTask(String plugin, Object seq) {
			this.plugin = plugin;
			this.seq = seq;
			synchronized (timerTasks) {
				timerTasks.add(this);
			}
		}
		
		@Override
		public void run() {
			synchronized (timerTasks) {
				timerTasks.remove(this);
			}
			pluginProxy.sendMessage(plugin, new HashMap<String, Object>() {{
				put("seq", seq);
			}});
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
