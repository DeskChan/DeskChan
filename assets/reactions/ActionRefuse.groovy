class ActionRefuse {

    def proxy
    def chance

    def refuse = { sender, tag, data ->
        if (Math.random() < chance)
            proxy.sendMessage("DeskChan:request-say", [ intent: 'REFUSE' ])
        else
            proxy.sendMessage("DeskChan:user-said#reactions:refuse", data)
    }

    void initialize(proxy) {
        this.proxy = proxy
        proxy.setAlternative("DeskChan:user-said", "reactions:refuse", 110)
        proxy.addMessageListener("reactions:refuse", refuse)
    }

    void unload(proxy){
        proxy.deleteAlternative("DeskChan:user-said", "reactions:refuse")
        proxy.removeMessageListener("reactions:refuse", refuse)
    }

    void setPreset(def preset){
        chance = Math.max(
                 ( preset.get("selfconfidence") -
                   preset.get("attitude") -
                   preset.get("energy") -
                   preset.get("relationship")) * 0.0625,
                  0.0
        )
    }
}
