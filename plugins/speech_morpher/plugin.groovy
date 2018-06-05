log("loading speech_morpher plugin")

class Bridge {
    def split (String text){  return Spliter.split(text)     }
    def insert(String text, ArrayList insertions){  return Insertion.insert(text, insertions)  }
}

properties = getProperties()

class Module {
    String name
    def instance
    static int priority = 5000
    def proxy
    boolean active = true

    Module(path, proxy) throws Exception {
        this.proxy = proxy
        name = path.getName()
        if (name.indexOf('.') >= 0)
            name = name.substring(0, name.lastIndexOf('.'))

        try {
            instance = new GroovyClassLoader().parseClass(path).newInstance()
        } catch (Exception e){
            throw e
        }
        try {
            instance.initialize(new Bridge())
        } catch (Exception e){ }
        proxy.sendMessage("core:register-alternative",
                ["srcTag": "DeskChan:request-say", "dstTag": proxy.getId()+":"+name, "priority": priority--])

        proxy.addMessageListener( proxy.getId() + ":" + name, { sender, tag, data ->
            int usage = proxy.getProperties().getInteger(name, 1)
            if (usage == 2 || (usage == 1 && active))
                data = morphPhrase(data)
            proxy.sendMessage("DeskChan:request-say#" + proxy.getId() + ":" + name, data)
        })
    }

    void checkActive(Map preset){
        try {
            active = instance.checkActive(preset)
        } catch (Exception e){ active = true }
    }

    Map morphPhrase(Map phrase){
        try {
            return instance.morphPhrase(phrase)
        } catch (Exception e){
            proxy.log(new Exception("Cannot morph phrase " + phrase + " with module " + name, e))
            return phrase
        }
    }

    String getName(Locale locale){ return instance.getName(locale) }


}
modules = new ArrayList<Module>()

File dir = getAssetsDirPath().resolve("speech_morphers").toFile()
dir.listFiles().each {
    try {
        modules.add(new Module(it, this))
    } catch (Exception e){
        log(e)
    }
}

pluginValues = [ OFF: 0, BY_PRESET: 1, ALWAYS: 2 ]
void setupMenu(){
    def controls = []
    for (Module module : modules)
        controls.add([
             type: 'ComboBox', id: module.name, label: module.getName(), value: getProperties().getInteger(module.name, 1), values: pluginValues.keySet()
        ])
    sendMessage('gui:set-panel', [
            name: getString("options"),
            id: "options",
            type: 'submenu',
            msgTag: getId() + ":save-options",
            controls: controls,
            action: "set"
    ])
}

void setPreset(data){
    for (Module module : modules)
        module.checkActive(data)
}

addMessageListener("talk:character-updated", {sender, tag, data ->
    setPreset(data)

})
sendMessage("talk:get-preset", null, {sender, data ->
    setPreset(data)
})

properties.load()
setupMenu()

addMessageListener(getId() + ":save-options", {sender, tag, data ->
    for (def entry : data.entrySet()) {
        properties.put(entry.key, pluginValues[entry.value])
    }
    properties.save()
})

addMessageListener(getId() + ":supply-resource", {sender, tag, data ->
    for (def entry : data.entrySet())
        properties.put(entry.key, entry.value)
})

log("loading speech_morpher completed")
