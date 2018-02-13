class Insertion {
    static final def random = new Random(System.currentTimeMillis())
    static def obsceneInsertions = [
        "Бля",
        "Блять",
        "Нахуй",
        "Пиздец",
        "Eбать",
    ]
    static def nekoInsertions = [
        "Ня",
        "Нян",
        "Мур",
        "Мяу",
    ]
    static def positions = [
        // Don't change the order!
        "beginning",
        "end",
        "middle",
    ]

    static void initialize(pluginName, pluginData) {
        def instance = pluginData.instance
        final def partTag = pluginName + ":insertion"

        instance.sendMessage("core:register-alternative",
                             ["srcTag": "DeskChan:request-say", "dstTag": partTag, "priority": 5000 ])

        instance.addMessageListener(pluginName + ":insertion", {sender, tag, data ->
            def phrase = data
            def text = data.get("text")
            def firstLoop = true
            if(pluginData.nekonization || pluginData.obscenization)
                text = insert(text, pluginData.nekonization, pluginData.obscenization, firstLoop)
            phrase.replace("text", text)
            phrase.replace("priority", 4999)
            instance.sendMessage("DeskChan:request-say#" + partTag, phrase)
        })
    }

    static def insert(text, nekonization, obscenization, firstLoop) {
        def insertions
        if(nekonization && firstLoop)
            insertions = nekoInsertions
        else if(obscenization && !firstLoop)
            insertions = obsceneInsertions
        else
            return text

        def textParts = Spliter.split(text)
        def position
        if(textParts.size() >= 2)
            position = positions[random.nextInt(positions.size())]
        else
            position = positions[random.nextInt(positions.size()-1)]
        def insertion = insertions[random.nextInt(insertions.size())]

        def finalText = ""
        if(position == "beginning")
            finalText = insertion + ". " + text

        else if(position == "end")
            finalText = text + " " + insertion + text[-1]

        else if (position == "middle") {
            def inTextPosition = 0
            if (textParts.size()<=2)
                inTextPosition = 0
            else {
                inTextPosition = random.nextInt(textParts.size() - 2) // "-2" is protect against "end" position.
            }
            for(def i = 0; i < textParts.size(); i++) {
                finalText += textParts[i]
                if (i == inTextPosition) {
                    if(textParts[i][-1] == "." && textParts[i][-2] == "." && textParts[i][-3])
                        insertion += ".."
                    else if(textParts[i][-1] == ',')
                        insertion = insertion.toLowerCase()
                    finalText += " " + insertion + finalText[-1]
                }
            }
        }
        if(firstLoop && obscenization) {
            firstLoop = false
            return insert(finalText, nekonization, obscenization, firstLoop)
        }
        else
            return finalText
    }
}