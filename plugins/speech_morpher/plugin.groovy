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

File dir = getPluginDirPath().resolve("modules").toFile()
dir.listFiles().each {
    try {
        modules.add(new Module(it, this))
    } catch (Exception e){
        log(e)
    }
}
/*
class PluginData {
    def instance
    def preset
    def properties

    def sumOfCharacteristic
    def nekonization
    def obscenization
    def powerOfStuttering = "none"
    def culturizationMod = "none"
    def exclamentionMod = "none"

    PluginData(instance,properties) {
        this.instance = instance
        this.properties = properties
    }

    void calculatePluginMode() {
        def tags = preset.get("tags")

        nekonization = false
        obscenization = false
        powerOfStuttering = "none"
        culturizationMod = "none"
        exclamentionMod = "none"

        sumOfCharacteristic = preset.get("selfconfidence") + preset.get("energy") + preset.get("attitude")
        +preset.get("impulsivity") + preset.get("relationship")

        if (preset.get("energy") >= -1 && preset.get("manner") <= 2 && preset.get("impulsivity") != -4
                && properties.getBoolean('enableNekonization'))
        {
            nekonization = true
            instance.log("nekonization was enabled")
        }

        if (preset.get("manner") <= -2
                && properties.getBoolean("enableObscenization")) {
            obscenization = true
            instance.log("obscenization was enabled")
        }

        if (preset.get("manner") <= -2
                && properties.getBoolean("enableUnculturization")) {
            culturizationMod = "unculture"
            instance.log("unculture was enabled")
        } else if (preset.get("manner") >= 2
                && properties.getBoolean("enableCulturization")) {
            culturizationMod = "culture"
            instance.log("culture was enabled")
        }

        if (preset.get("manner") <= 2 && preset.get("manner") >= -2
                && properties.getBoolean("enableStuttering")) {
            if (sumOfCharacteristic <= 6 && sumOfCharacteristic > 2) {
                powerOfStuttering = "light"
                instance.log("powerOfStuttering is light")
            }
            else if (sumOfCharacteristic <= 2 && sumOfCharacteristic > -2) {
                powerOfStuttering = "perceptible"
                instance.log("powerOfStuttering is perceptible")
            }
            else if (sumOfCharacteristic <= -2 && sumOfCharacteristic > -8) {
                powerOfStuttering = "irritable"
                instance.log("powerOfStuttering is irritable")
            }
        }

        if (preset.get("impulsivity") >= 2 && properties.getBoolean("enableExclamention")) {
            exclamentionMod = "exclamention"
        } else if (preset.get("impulsivity") <= -2
                && properties.getBoolean("enableUnexclamention"))
        {
            exclamentionMod = "unexclamention"
            instance.log("exclamentionMod is unexclamention")
        }
    }
}
*/

void setupMenu(){
    def controls = []
    for (Module module : modules)
        controls.add([
             type: 'ComboBox', id: module.name, label: module.getName(), value: getProperties().getInteger(module.name, 1), values: ["OFF", "BY_PRESET", "ALWAYS"]
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
    for (def entry : data.entrySet())
        properties.put(entry.key, entry.value)
    properties.save()
})

addMessageListener(getId() + ":supply-resource", {sender, tag, data ->
    for (def entry : data.entrySet())
        properties.put(entry.key, entry.value)
})

log("loading speech_morpher completed")
