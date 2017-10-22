package info.deskchan.core_utils;

import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core.ResponseListener;

import java.util.HashMap;

public abstract class CoreTimerTask implements ResponseListener, Runnable{

    protected Object lastSeq = null;
    protected PluginProxyInterface proxy;
    protected long delay;
    protected boolean repeat;

    public CoreTimerTask(PluginProxyInterface pluginProxy, long delay, boolean repeat){
        proxy = pluginProxy;
        this.delay = delay;
        this.repeat = repeat;
    }

    @Override
    public void handle(String sender, Object data) {
        run();
        if(!repeat) return;
        lastSeq = null;
        start();
    }

    public void start() {
        if (lastSeq != null) stop();
        lastSeq = proxy.sendMessage("core-utils:notify-after-delay", TextOperations.toMap("delay: "+delay), this);
    }

    public void stop() {
        if (lastSeq != null)
        proxy.sendMessage("core-utils:notify-after-delay", new HashMap<String, Object>() {{
            put("seq", lastSeq);
            put("delay", (long) -1);
        }});
    }
};