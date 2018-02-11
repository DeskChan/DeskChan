import java.nio.file.Files

def pluginName = getId()

def subpluginEnableList = [
    "enableNekonization",
    "enableObscenization",
    "enableCulturization",
    "enableUnculturization",
    "enableStuttering",
    "enableExclamention",
    "enableUnexclamention",
]

class PluginData {
    def instance
    def sumOfCharacteristic
    def nekonization
    def obscenization
    def powerOfStuttering = "none"
    def culturizationMod = "none"
    def exclamentionMod = "none"

    PluginData(instance, preset, properties) {
        this.instance = instance
        def tags = preset.get("tags")
        sumOfCharacteristic = preset.get("selfconfidence") + preset.get("energy") + preset.get("attitude")
        + preset.get("impulsivity") + preset.get("relationship")

        if(tags.containsKey("species") && tags.get("species").contains("neko")
                && preset.get("energy") >= -1 && preset.get("manner") <= 2 && preset.get("impulsivity") != -4
                && properties.getBoolean('enableNekonization'))
            nekonization = true

        if(preset.get("manner") <= -2
                && properties.getBoolean("enableObscenization"))
            obscenization = true

        if(preset.get("manner") <= -2
                && properties.getBoolean("enableUnculturization"))
            culturizationMod = "unculture"
        else if(preset.get("manner") >= 2
                && properties.getBoolean("enableCulturization"))
            culturizationMod = "culture"

        if(preset.get("manner") <= 2 && preset.get("manner") >= -2
                && properties.getBoolean("enableStuttering")) {
            if(sumOfCharacteristic <= 6 && sumOfCharacteristic > 2)
                powerOfStuttering = "light"
            else if(sumOfCharacteristic <= 2 && sumOfCharacteristic > -2)
                powerOfStuttering = "perceptible"
            else if(sumOfCharacteristic <= -2 && sumOfCharacteristic > -8)
                powerOfStuttering = "irritable"
        }

        if(preset.get("impulsivity") >= 2
                && properties.getBoolean("enableExclamention"))
            exclamentionMod = "exclamention"
        else if(preset.get("impulsivity") <= -2
                && properties.getBoolean("enableUnexclamention"))
            exclamentionMod = "unexclamention"
    }
}

def setSettings(enableList) {
    def configPath = getDataDirPath().resolve("config.properties")
    if(!Files.exists(configPath)) {
        for(String entry : subpluginEnableList)
            getProperties().putIfHasNot(entry, true)
    }
    if(enableList) {
         for(String entry : subpluginEnableList)
            getProperties().put(entry, enableList.get(entry))
    }
    getProperties().save()
}

def preset
addMessageListener("talk:character-updated", {sender, tag, data ->
    preset = data
})

sendMessage("talk:get-preset", null, {sender, data ->
    preset = data
})

getProperties().load()
setSettings()
sendMessage('gui:setup-options-submenu', [name: getString("options"), msgTag: pluginName + ":options", controls: [
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

addMessageListener(pluginName + ":options", {sender, tag, data ->
    def enableList = [:]
    for(String entry : subpluginEnableList)
        enableList.put(entry, data.get(entry))
    setSettings(enableList)
})

addMessageListener(pluginName + ":supply-resource", {sender, tag, data ->
    def enableList = [:]
    for(String entry : subpluginEnableList) {
        if(data.contains(entry))
            enableList.put(entry, data.get(entry))
    }
    setSettings(enableList)
})

def pluginData = new PluginData(this, preset, getProperties())
Insertion.initialize(pluginName, pluginData)
Exclamention.initialize(pluginName, pluginData)
Culturing.initialize(pluginName, pluginData)
Stuttering.initialize(pluginName, pluginData)
