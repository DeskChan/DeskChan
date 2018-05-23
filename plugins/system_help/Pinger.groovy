class Pinger{

    static void initialize(pluginName, data) {
        def instance = data.instance

        instance.sendMessage("talk:add-plugin-phrases", instance.getPluginDirPath().resolve('system.phrases'))
        instance.sendMessage("core:add-command", [tag: pluginName + ':ping'])

        instance.addMessageListener(pluginName + ':ping', { sender, tag, d ->
            Thread.start(){
                def address = InetAddress.getByName("4.4.4.4")
                instance.sendMessage("DeskChan:request-say", address.isReachable(2000) ? "NO_NETWORK" : "NETWORK_OK")
            }
        })
    }
}
