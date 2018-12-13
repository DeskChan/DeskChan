import java.nio.charset.StandardCharsets

setResourceBundle("resources")

def dir = getPluginDirPath().resolve("radio")

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

def getNamesList(){
    def res = new HashMap()
    radioMap.each { res[it.name] = it.path.toString() }
    return res
}

sendMessage("core:add-command", [
        tag: 'radio:play',
        info: getString('command-info'),
        msgInfo: getString('command-msg-info')
])

addMessageListener('radio:play', { sender, tag, data ->
    println("opening scenario")
    sendMessage("scenario:run-scenario", [
            path: getPluginDirPath().resolve("open-radio.scenario").toString(),
            giveOwnership: true,
            msgData: [
                    "radioList": getNamesList(),
                    "msgData": data["msgData"]
            ]
    ])
})

addMessageListener('radio:save-radio', { sender, tag, data ->
    saveRadio(data)
})

addMessageListener("recognition:get-words", {sender, tag, d ->
    HashSet<String> set = new HashSet<>()
    for (Radio r : radioMap) set.add(r.name)
    sendMessage(sender, set)
})

def saveRadio(Map data){
    def filename = data["filename"], url = data["url"]
    def file = dir.resolve(filename + ".m3u")
    file.createNewFile()

    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)
    writer.write(url)
    writer.close()
    filename = name.getName().substring(0, name.indexOf('.'))
    radioMap[filename] = file
    sendMessage("core:open-link", file)
}

// здесь мы связываем команду и событие
sendMessage("core:set-event-link",
        [
                eventName: 'speech:get',
                commandName: 'radio:play',
                rule: getString('radio-rule')
        ]
)
