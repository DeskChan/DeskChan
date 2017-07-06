package info.deskchan.core;

import java.nio.file.Path;
import java.util.*;

public class PluginProxy implements MessageListener {
	
	private final Plugin plugin;
	private String id = null;
	private Map<String, Set<MessageListener>> messageListeners = new HashMap<>();
	private Map<Object, ResponseInfo> responseListeners = new HashMap<>();
	private int seq = 0;

	PluginProxy(Plugin plugin) {
		this.plugin = plugin;
	}

	public String getId() {
		return id;
	}

	public boolean initialize(String id) {
		assert this.id == null;
		this.id = id;
		addMessageListener(id, this);
		return plugin.initialize(this);
	}

	public void unload() {
		assert id != null;
		try {
			plugin.unload();
		} catch (Throwable e) {
			log(e);
		}
		for (Map.Entry<String, Set<MessageListener>> entry : messageListeners.entrySet()) {
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

	public Object sendMessage(String tag, Object data, ResponseListener responseListener, ResponseListener returnListener) {
		if (!(data instanceof Map)) {
			Map<String, Object> m = new HashMap<>();
			m.put("data", data);
			data = m;
		}
		Map<String, Object> m = (Map<String, Object>) data;
		Object seq = m.getOrDefault("seq", null);
		if (seq == null) {
			seq = this.seq++;
		}

		m.put("seq", seq);
		int count=PluginManager.getInstance().getMessageListenersCount(tag);
		if(count>0)
			responseListeners.put(seq, new ResponseInfo(responseListener,count,returnListener));
		else returnListener.handle(id,null);
		sendMessage(tag, data);
		return seq;
	}
	public Object sendMessage(String tag, Object data, ResponseListener responseListener) {
		if (!(data instanceof Map)) {
			Map<String, Object> m = new HashMap<>();
			m.put("data", data);
			data = m;
		}
		Map<String, Object> m = (Map<String, Object>) data;
		Object seq = m.getOrDefault("seq", null);
		if (seq == null) {
			seq = this.seq++;
		}
		m.put("seq", seq);
		int count=PluginManager.getInstance().getMessageListenersCount(tag);
		if(count>0)
			responseListeners.put(seq, new ResponseInfo(responseListener,count));
		sendMessage(tag, data);
		return seq;
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

	@Override
	public void handleMessage(String sender, String tag, Object data) {
		if (data instanceof Map) {
			Map<String, Object> m = (Map<String, Object>) data;
			Object seq = m.getOrDefault("seq", null);
			if(seq==null) return;
			ResponseInfo listener = responseListeners.getOrDefault(seq,null);
			if(listener==null) return;
			if(listener.handle(sender, id, data)){
				responseListeners.remove(seq);
			}
		}
	}
	private static ResourceBundle general_strings = null;

	static {
		try {
			general_strings = ResourceBundle.getBundle("info/deskchan/strings");
		} catch(Exception e){
			PluginManager.log("Cannot find resource bundle info/deskchan/strings");
		}
	}

	public ResourceBundle plugin_strings = null;

	public final String getString(String key){
		String s=key;
		if(general_strings!=null && general_strings.containsKey(key))
			s=general_strings.getString(key);
		else if(plugin_strings!=null && plugin_strings.containsKey(key))
			s=plugin_strings.getString(key);
		try{
			return new String(s.getBytes("ISO-8859-1"), "UTF-8");
		} catch(Exception e){
			return s;
		}
	}

	public Path getRootDirPath() {
		return PluginManager.getRootDirPath();
	}

	public Path getDataDirPath() {
		return PluginManager.getPluginDataDirPath(id);
	}

	public void log(String text) {
		PluginManager.log(id, text);
	}

	public void log(Throwable e) {
		PluginManager.log(id, e);
	}

}

class ResponseInfo{
	private int count;
	private ResponseListener res;
	private ResponseListener ret;
	public ResponseInfo(ResponseListener responseListener,int count){
		this.count=count;
		res=responseListener;
		ret=null;
	}
	public ResponseInfo(ResponseListener responseListener,int count,ResponseListener returnListener){
		this.count=count;
		res=responseListener;
		ret=returnListener;
	}
	public boolean handle(String sender, String id,Object data){
		res.handle(sender,data);
		count--;
		if(count==0) {
			if(ret!=null) ret.handle(id, null);
			return true;
		}
		return false;
	}
}