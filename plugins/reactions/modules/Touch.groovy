class Touch {

    def proxy

    def touch = { sender, tag, data ->
        proxy.sendMessage("DeskChan:request-say", [ purpose: 'TOUCH', priority: 2000, timeout: 1000 ])
    }

    void initialize(proxy) {
        this.proxy = proxy
        proxy.addMessageListener("gui-events:character-left-click", touch)
    }

    void unload(proxy){
        proxy.removeMessageListener("gui-events:character-left-click", touch)
    }

    void setPreset(def preset){ }
}
