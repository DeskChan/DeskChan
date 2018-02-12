import java.nio.file.Files

pluginName = "speech_morpher"

log("loading speech_morpher plugin");

subpluginEnableList = [
    "enableNekonization",
    "enableObscenization",
    "enableCulturization",
    "enableUnculturization",
    "enableStuttering",
    "enableExclamention",
    "enableUnexclamention"
]

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

def setSettings(enableList) {
    def properties = getProperties()
    def configPath = getDataDirPath().resolve("config.properties")
    if(!Files.exists(configPath)) {
        for(String entry : subpluginEnableList)
            properties.putIfHasNot(entry, false)
    }
    if(enableList) {
         for(String entry : subpluginEnableList)
             properties.put(entry, enableList.get(entry))
    }
    getProperties().save()
}

void setupMenu(){
    sendMessage('gui:setup-options-submenu', [
            name: getString("options"),
            msgTag: pluginName + ":save-options",
            controls: [
            [
                    type: 'CheckBox', id: 'enableNekonization', label: 'Включить неко-вставки',
                    value: getProperties().getBoolean('enableNekonization')
            ],
            [
                    type: 'CheckBox', id: 'enableObscenization', label: 'Включить обсценые вставки',
                    value: getProperties().getBoolean('enableObscenization')
            ],
            [
                    type: 'CheckBox', id: 'enableExclamention', label: 'Включить усиление восклицания',
                    value: getProperties().getBoolean('enableExclamention')
            ],
            [
                    type: 'CheckBox', id: 'enableUnexclamention', label: 'Включить ослабление восклицания',
                    value: getProperties().getBoolean('enableUnexclamention')
            ],
            [
                    type: 'CheckBox', id: 'enableCulturization', label: 'Включить автозамену на литературные аналоги',
                    value: getProperties().getBoolean('enableCulturization')
            ],
            [
                    type: 'CheckBox', id: 'enableUnculturization', label: 'Включить автозамену на разговорные аналоги',
                    value: getProperties().getBoolean('enableUnculturization')
            ],
            [
                    type: 'CheckBox', id: 'enableStuttering', label: 'Включить заикание',
                    value: getProperties().getBoolean('enableStuttering')
            ]
    ]])
}

Map preset

addMessageListener("talk:character-updated", {sender, tag, data ->
    preset = data
})
sendMessage("talk:get-preset", null, {sender, data ->
    preset = data
})

getProperties().load()
setupMenu()

def pluginData = new PluginData(this, getProperties())
pluginData.preset = preset
pluginData.calculatePluginMode()

addMessageListener(pluginName + ":save-options", {sender, tag, data ->
    def enableList = [:]
    for(String entry : subpluginEnableList){
        enableList.put(entry, data.get(entry))
    }
    setSettings(enableList)
    pluginData.calculatePluginMode()
})

addMessageListener(pluginName + ":supply-resource", {sender, tag, data ->
    def enableList = [:]
    for(String entry : subpluginEnableList) {
        if(data.contains(entry))
            enableList.put(entry, data.get(entry))
    }
    setSettings(enableList)
})


Insertion.initialize(pluginName, pluginData)
Exclamention.initialize(pluginName, pluginData)
Culturing.initialize(pluginName, pluginData)
Stuttering.initialize(pluginName, pluginData)
log("loading speech_morpher completed");
