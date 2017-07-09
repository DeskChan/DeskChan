package info.deskchan.core;

import java.nio.file.Path;

public interface PluginProxyInterface extends MessageListener {

    String getId();

    void sendMessage(String tag, Object data);

    Object sendMessage(String tag, Object data, ResponseListener responseListener, ResponseListener returnListener);

    Object sendMessage(String tag, Object data, ResponseListener responseListener);

    void addMessageListener(String tag, MessageListener listener);

    void removeMessageListener(String tag, MessageListener listener);

    void setResourceBundle(String path);

    String getString(String key);

    Path getRootDirPath();

    Path getDataDirPath();

    void log(String text);

    void log(Throwable e);
}
