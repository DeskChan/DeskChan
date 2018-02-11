class Culturing {
    static def random = new Random(System.currentTimeMillis())
    static def listForReplase = [
        ['Сейчас', 'Щас'],
        ['Магазин', 'Магаз'],
        ['Что', 'Чё', 'Чо', 'Шо'],
        ['Cпасибо', 'Сенкс', 'Спс'],
        ['Тебе', 'Те'],
        ['Тебя', 'Тя'],
        ['Нормально', 'Норм'],
        ['Пожалуйста', 'Плез', 'Пжлст', 'Пож'],
        ['Телевизор', 'Телек', 'Тв', 'Ящик'],
        ['Компьютер', 'Пк', 'Пека', 'Компухтер', 'Кудкудахтер', 'Компилястер'],
        ['Быстро', 'Быро'],
    ]

    static void initialize(pluginName, pluginData) {
        def instance = pluginData.instance
        final def partTag = pluginName + ":culturing"

        instance.sendMessage("core:register-alternative",
                ["srcTag": "DeskChan:request-say", "dstTag": partTag, "priority": 4998])

        instance.addMessageListener(pluginName + ":culturing", {sender, tag, data ->
            def phrase = data
            def text = data.get("text")
            if(pluginData.culturizationMod != "none")
                text = culturise(text, pluginData.culturizationMod)
            phrase.replace("text", text)
            phrase.replace("priority", 4997)
            instance.sendMessage("DeskChan:request-say#" + partTag, phrase)
        })
    }

    static def culturise(text, mod) {
        def words = text.split(" ")
        def finalText = ""
        nextWord:
        for(def word : words) {
            def wordWithoutLastSymbols
            if(word.contains("!!!") || word.contains("..."))
                wordWithoutLastSymbols = word[0..-4]
            else if(word.contains("?!"))
                wordWithoutLastSymbols = word[0..-3]
            else if(word.contains("!") || word.contains("?") || word.contains(".") || word.contains(","))
                wordWithoutLastSymbols = word[0..-2]
            else
                wordWithoutLastSymbols = word

            char firstLetter = word[0]
            for(def i : listForReplase) {
                if(i.contains(wordWithoutLastSymbols.capitalize())) {
                    if(mod == "culture") {
                        finalText += firstLetter.isLowerCase() \
                                   ? word.replace(wordWithoutLastSymbols, i[0].toLowerCase()) \
                                   : word.replace(wordWithoutLastSymbols, i[0])
                        continue nextWord
                    }
                    else if(mod == "unculture") {
                        finalText += firstLetter.isLowerCase() \
                                   ? " " + word.replace(wordWithoutLastSymbols, i[random.nextInt(i.size()-1)+1].toLowerCase()) \
                                   : " " + word.replace(wordWithoutLastSymbols, i[random.nextInt(i.size()-1)+1])
                        continue nextWord
                    }
                }
            }
            finalText += " " + word
        }
        return finalText
    }
}