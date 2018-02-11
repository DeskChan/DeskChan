class Stuttering {
    static def random = new Random(System.currentTimeMillis())

    static void initialize(pluginName, pluginData) {
        def instance = pluginData.instance
        final def partTag = pluginName + ":stuttering"

        instance.sendMessage("core:register-alternative",
                             ["srcTag": "DeskChan:request-say", "dstTag": partTag, "priority": 4997])

        instance.addMessageListener(pluginName + ":stuttering", {sender, tag, data ->
            def phrase = data
            def text = data.get("text")
            if(pluginData.powerOfStuttering != "none")
                text = stutter(text, pluginData.powerOfStuttering)
            phrase.replace("text", text)
            phrase.replace("priority", 4996)
            instance.sendMessage("DeskChan:request-say#" + partTag, phrase)
        })
    }

    static def stutter(text, power) {
        def positions = []
        def words = text.split(" ")
        def hesitationCount
        def doubleHesitation
        if(power == "light") {
            hesitationCount = words.size().intdiv(11)
            doubleHesitation = false
        }
        else if (power == "perceptible") {
            hesitationCount = words.size().intdiv(5)
            doubleHesitation = false
        }
        else if(power == "irritable") {
            hesitationCount = words.size().intdiv(3)
            doubleHesitation = true
        }

        if(hesitationCount == 0)
            hesitationCount = 1

        for(def i : hesitationCount)
            positions.add(random.nextInt(words.size()))

        def finalText = ""
        for(int i = 0; i < words.size(); i++) {
            if(doubleHesitation && positions.contains(i))
                finalText += words[i][0] + "-" + words[i][0].toLowerCase() + "-" + words[i].toLowerCase() + " "
            else if (!doubleHesitation && positions.contains(i))
                finalText += words[i][0] + "-" + words[i].toLowerCase() + " "
            else
                finalText += words[i] + " "
        }
        return finalText
    }
}