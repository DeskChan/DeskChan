package info.deskchan.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyCore extends CorePlugin implements Plugin, MessageListener {

    @Override
    public boolean initialize(PluginProxyInterface pluginProxy) {
        boolean initializeStatus = super.initialize(pluginProxy);

        pluginProxy.addMessageListener("core:debug-output-alternatives", (sender, tag, data) -> {
            pluginProxy.sendMessage(sender, new HashMap<String, List<AlternativeInfo>>(alternatives));
        });

        return initializeStatus;
    }
}
