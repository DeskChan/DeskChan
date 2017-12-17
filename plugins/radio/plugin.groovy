import java.nio.charset.StandardCharsets

sendMessage("core:add-command", [ tag: 'radio:play' ])

addMessageListener('radio:play', { sender, tag, data ->
    if (data instanceof Map)
        playRadio(data.get('msgData'))
    else
        playRadio(data)
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

def clarify(){
    sendMessage('DeskChan:request-say', 'CLARIFY')
    sendMessage('DeskChan:request-user-speech', null, { s, d ->
        playRadio(d.get('msgData').toString())
    })
}

def playRadio(Object text){
    if (text == null || text.toString().length() == 0){
        clarify()
        return
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
            sendMessage("system:open-link", file.toString())
            sendMessage('DeskChan:say', 'А как оно называется?')
            sendMessage('DeskChan:request-user-speech', null, { s2, d2 ->
                saveRadio(d2.get('msgData'), text)
            })
            return
        }
        variants = ''
        radioMap.each { k ->
            variants += k.name + ', '
        }
        variants += 'ну и всё на этом.'
        sendMessage('DeskChan:request-say', 'WRONG_DATA')
        sendMessage('DeskChan:say', 'Вот те радиостанции, что я знаю: ' + variants)
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
                rule: 'радио {msgData:Text}'
        ]
)
