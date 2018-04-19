class ActionRefuse {

    def proxy
    def chance

    def refuse = { sender, tag, data ->
        if (Math.random() < chance)
            proxy.sendMessage("DeskChan:request-say", [ purpose: 'REFUSE' ])
        else
            proxy.sendMessage("DeskChan:user-said#reactions:refuse", data)
    }

    def alternativeMap = [
            "srcTag": "DeskChan:user-said",
            "dstTag": "reactions:refuse",
            "priority": 110
    ]

    void initialize(proxy) {
        this.proxy = proxy
        proxy.sendMessage("core:register-alternative", alternativeMap)
        proxy.addMessageListener("reactions:refuse", refuse)
    }

    void unload(proxy){
        proxy.sendMessage("core:unregister-alternative", alternativeMap)
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
