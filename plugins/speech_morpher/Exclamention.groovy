class Exclamention {
    static void initialize(pluginName, pluginData) {
        def instance = pluginData.instance
        final def partTag = pluginName + ":exclamention"

        instance.sendMessage("core:register-alternative",
                             ["srcTag": "DeskChan:request-say", "dstTag": partTag, "priority": 4999 ])

        instance.addMessageListener(partTag, {sender, tag, data ->
            def phrase = data
            def text = data.get("text")
            if(pluginData.exclamentionMod != "none")
                text = exclaim(text, pluginData.exclamentionMod)
            phrase.replace("text", text)
            instance.sendMessage("DeskChan:request-say#" + partTag, phrase)
        })
    }

    static def exclaim(text, mod) {
        def textParts = Spliter.split(text)
        def finalText = ""
        def bufString
        for(def i : textParts) {
            if(mod == "exclamention") {
                if(!i.contains("...") && (i.contains(".") || i.contains("!") || i.contains("?"))) {
                    if(i.contains("."))
                        i = i.replace(".", "!")
                    else if(i.contains("!"))
                        i = i.replace("!", "!!!")
                    else if(i.contains("?"))
                        i = i.replace("?", "?!")
                }
            }
            else if(mod == "unexclamention") {
                if(i.contains("!") || i.contains("!!!") || i.contains("!?")) {
                    if(i.contains("!!!"))
                        i = i.replace("!!!", "!")
                    else if(i.contains("!?"))
                        i = i.replace("!?", "?")
                    else if(i.contains("!"))
                        i = i.replace("!", ".")

                }
            }
            finalText += i
        }
        return finalText
    }
}