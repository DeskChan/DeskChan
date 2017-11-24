package info.deskchan.groovy_support;

import groovy.lang.Script;
import info.deskchan.core.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class GroovyPlugin extends Script implements Plugin {
	
	private PluginProxyInterface pluginProxy = null;
	private List<Runnable> cleanupHandlers = new ArrayList<>();
	private Path pluginDirPath;
	
	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		this.pluginProxy = pluginProxy;
		try {
			run();
		} catch (Exception e) {
			pluginProxy.log(e);
			return false;
		}
		return true;
	}
	
	@Override
	public void unload() {
		for (Runnable runnable : cleanupHandlers) {
			runnable.run();
		}
	}

	protected String getId() {
		return pluginProxy.getId();
	}
	
	protected void sendMessage(String tag, Object data) {
		pluginProxy.sendMessage(tag, data);
	}
	
	protected void sendMessage(String tag, Object data, ResponseListener responseListener) {
		pluginProxy.sendMessage(tag, data, responseListener);
	}

	protected void sendMessage(String tag, Object data, ResponseListener responseListener, ResponseListener returnListener) {
		pluginProxy.sendMessage(tag, data, responseListener, returnListener);
	}

	protected void addMessageListener(String tag, MessageListener listener) {
		pluginProxy.addMessageListener(tag, listener);
	}

	protected void removeMessageListener(String tag, MessageListener listener) {
		pluginProxy.removeMessageListener(tag, listener);
	}

	protected int setTimer(long delay, ResponseListener listener) {
		return pluginProxy.setTimer(delay, listener);
	}

	protected int setTimer(long delay, int count, ResponseListener listener) {
		return pluginProxy.setTimer(delay, count, listener);
	}

	protected void cancelTimer(int id) {
		pluginProxy.cancelTimer(id);
	}

	protected String getString(String key){ return pluginProxy.getString(key); }

	protected void addCleanupHandler(Runnable handler) {
		cleanupHandlers.add(handler);
	}

	protected PluginProperties getProperties() {
		return pluginProxy.getProperties();
	}

	protected Path getDataDirPath() {
		return pluginProxy.getDataDirPath();
	}
	
	protected Path getPluginDirPath() {
		return pluginDirPath;
	}

	protected void setResourceBundle(String path){ pluginProxy.setResourceBundle(getPluginDirPath().resolve(path).toString()); }

	protected void setConfigField(String key, Object value){ pluginProxy.setConfigField(key,value); }

	protected Object getConfigField(String key){ return pluginProxy.getConfigField(key); }
	
	protected void log(String text) {
		pluginProxy.log(text);
	}
	
	protected void log(Throwable e) {
		pluginProxy.log(e);
	}
	
	void setPluginDirPath(Path path) {
		pluginDirPath = path;
	}
	
}
