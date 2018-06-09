log("loading reactions plugin")

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
            def filetext = path.getText('UTF-8')
            instance = new GroovyClassLoader().parseClass(filetext, name).newInstance()
        } catch (Exception e) {
            throw e
        }
        try {
            instance.initialize(proxy)
        } catch (MissingMethodException e ) {
        } catch(Exception e){
            proxy.log(e)
        }
    }

    void setActive(boolean val){
        if (val == active) return
        active = val
        if (active) instance.initialize(proxy)
        else instance.unload(proxy)
    }

    void setPreset(def preset){
        instance.setPreset(preset)
    }
}

modules = new ArrayList<Module>()

File dir = getAssetsDirPath().resolve("reactions").toFile()
dir.listFiles().each {
    try {
        modules.add(new Module(it, this))
    } catch (Exception e) {
        log(e)
    }
}

void setupMenu() {
    def controls = []
    for (Module module : modules)
        controls.add([
            type: 'CheckBox', id: module.name, label: module.getName(), value: getProperties().getBoolean(module.name, true)
        ])
    sendMessage('gui:set-panel', [
        name: getString("options"),
        id: "options",
        type: "submenu",
        msgTag: getId() + ":save-options",
        controls: controls,
        action: "set"
    ])
}

void setPreset(data){
    for (Module module : modules)
        module.setPreset(data)
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
    for (def entry : data.entrySet()){
        for (Module module : modules){
            if (entry.key == module.name) {
                module.setActive(entry.value)
                break
            }
        }
        properties.put(entry.key, entry.value)
    }

    properties.save()
})

addMessageListener(getId() + ":supply-resource", {sender, tag, data ->
    for(def entry : data.entrySet())
    properties.put(entry.key, entry.value)
})

log("loading reactions complited")
