package info.deskchan.core;

public interface TypedMessageListener<T> {
	void handleMessage(String sender, String tag, T data);
}
