import java.nio.charset.StandardCharsets

sendMessage("core:add-command", [
        tag: 'radio:play',
        info: getString('command-info'),
        msgInfo: getString('command-msg-info')
])

addMessageListener('radio:play', { sender, tag, data ->
    playRadio( data instanceof Map ? data.get('msgData') : data.toString())
})

def dir = getPluginDirPath().resolve("radio").toFile()

class Radio{
    String name
    String rule
    def path
    Radio(it){
        name = it.getName()
        name = name.substring(0, name.indexOf('.'))
        rule = name.replaceAll("(^|\\s)([A-я])", ' ?$2')
        path = it
    }
}
radioMap = new ArrayList<>()
dir.eachFileRecurse {
    radioMap.add(new Radio(it))
}

addMessageListener("recognition:get-words", {sender, tag, d ->
    HashSet<String> set = new HashSet<>()
    for (Radio r : radioMap) set.add(r.name)
    sendMessage(sender, set)
})

def clarify(){
    sendMessage('DeskChan:request-say', 'CLARIFY')
    sendMessage('DeskChan:request-user-speech', printVariants(), { s, d ->
        playRadio(d.get('value').toString())
    })
}

def printVariants() {
    // печать вариантов вынесена в отдельную функцию
    variants = []
    radioMap.each { k ->
        variants += k.name
    }
    //variants += 'ну и всё на этом.'
    return variants
    //sendMessage('DeskChan:say', 'Вот те радиостанции, что я знаю: ' + variants)
}

def playRadio(Object text){
    if (text == null || text.toString().length() == 0){
        printVariants()
        clarify()
        return // не работает или я что-то не понял
    }
    path = null
    list = new ArrayList<String>()
    radioMap.each { it ->
        list += it.rule
    }
    sendMessage("speech:match-any", ["speech": text, "rules": list], {s, d ->
        int result = d
        if (result >= 0){
            sendMessage("system:open-link", radioMap[result].path.toString())
            sendMessage('DeskChan:request-say', 'DONE')
            return
        }
        if (text.contains("://")){
            saveRadio("temp.m3u", text)
            sendMessage("core:open-link", file.toString())
            sendMessage('DeskChan:say', getString('set-name-phrase'))
            sendMessage('DeskChan:request-user-speech', getString('write-radio-name'), { s2, d2 ->
                saveRadio(d2.get('value'), text)
            })
            return
        }
        sendMessage('DeskChan:request-say', 'WRONG_DATA')
        printVariants()
    })
}

def saveRadio(String filename, String url){
    def file = dir.resolve(filename)
    file.createNewFile()

    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)
    writer.write(url)
    writer.close()
    filename = name.getName().substring(0, name.indexOf('.'))
    radioMap[filename] = file
}

// здесь мы связываем команду и событие
sendMessage("core:set-event-link",
        [
                eventName: 'speech:get',
                commandName: 'radio:play',
                rule: getString('rule')
        ]
)
