import java.awt.Paint
import java.nio.charset.StandardCharsets

sendMessage("core:add-command", [ tag: 'radio:play' ])

addMessageListener('radio:play', { sender, tag, dat ->
    HashMap<String,Object> data = dat
    if(data.containsKey('value'))
        playRadio(data.get('value').toString())
    else {
        sendMessage('DeskChan:say', 'Что ты хочешь услышать, сахарочек?')
        sendMessage('DeskChan:request-user-speech', null, { s, d ->
            playRadio(d.get('value').toString())
        })
    }
})

def dir = getPluginDirPath().resolve("radio").toFile()
radioMap = new HashMap<String, String>()
dir.eachFileRecurse {
    name = it.getName()
    name = name.substring(0, name.indexOf('.'))
    radioMap[name] = it
}

def playRadio(String text){
    replaced = text.replace("[^A-zА-я\\s]","").replace("\\s+", " ")
    path = null
    radioMap.each{ k, v ->
        if (k.contains(replaced)) path = v
    }
    if(path != null) {
        sendMessage("system:open-link", path.toString())
        return
    }
    if (text.contains("://")){
        saveRadio("temp.m3u", text)
        sendMessage("system:open-link", file.toString())
        sendMessage('DeskChan:say', 'А как оно называется?')
        sendMessage('DeskChan:request-user-speech', null, { s, d ->
            saveRadio(d.get('value'), text)
        })
        return
    }
    variants = ''
    radioMap.each { k, v ->
        variants += k + ', '
    }
    variants += 'ну и всё на этом.'
    sendMessage('DeskChan:say', 'Я не знаю такой радиостанции. Зато смотри, что у меня есть: ' + variants)
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
                rule: 'включи радио {value:text}'
        ]
)
