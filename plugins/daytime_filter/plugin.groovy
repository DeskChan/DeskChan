import java.nio.file.Files

filename = getDataDirPath().resolve('config')
boolean isEnabled(){
    return Files.exists(filename)
}
void setEnabled(boolean value){
    if(value){
        try{
            Files.createFile(filename)
        } catch(Exception e){ }
    } else {
        try{
            Files.delete(filename)
        } catch(Exception e){ }
    }
}

// TODO: add a filter for morning
sunset = [red: 0.94, green: 0.82, blue: 1.0]
night = [red: 0.63, green: 0.78, blue: 0.82]
timerId = -1
def updateSkin() {
    if(!isEnabled()){
        cancelTimer(timerId)
        return
    }
    doDependingOnDaytime(
        night: { setSkinFilter(night) },
        evening: { setSkinFilter(sunset) }
    )
    timerId = setTimer(3600000, { sender, data ->
        updateSkin()
    })
}

static def doDependingOnDaytime(Map<String, Closure> daytimes) {
    int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    if (currentHour < 6) {
        return daytimes['night']?.call()
    }
    if (currentHour < 12) {
        return daytimes['morning']?.call()
    }
    if (currentHour < 17) {
        return daytimes['day']?.call()
    }
    if (currentHour < 23) {
        return daytimes['evening']?.call()
    }
    return daytimes['night']?.call()
}

def setSkinFilter(filter) {
    sendMessage('gui:set-skin-filter', filter)
}

addCleanupHandler { setSkinFilter(null) }
updateSkin()


// DEBUG SECTION
// TODO: delete this section after debugging

final tagApply = getId() + ':apply'


sendMessage('gui:setup-options-submenu', [name: getString("options"), msgTag: tagApply, controls: [
    [
        type: 'CheckBox', id: 'enabled', label: 'Фильтры по времени суток',
        value: isEnabled()
    ],
    [
        type: 'Label', label: 'Наложить свой фильтр:'
    ],
    [
        type: 'FloatSpinner', id: 'red', label: 'Красный канал',
        value: 1.0, min: 0.0, max: 1.0, step: 0.01
    ],
    [
        type: 'FloatSpinner', id: 'green', label: 'Зелёный канал',
        value: 1.0, min: 0.0, max: 1.0, step: 0.01
    ],
    [
        type: 'FloatSpinner', id: 'blue', label: 'Синий канал',
        value: 1.0, min: 0.0, max: 1.0, step: 0.01
    ],
    [
        type: 'FloatSpinner', id: 'opacity', label: 'Прозрачность',
        value: 1.0, min: 0.0, max: 1.0, step: 0.01
    ]
]])

addMessageListener(tagApply, { sender, tag, data ->
    m = (Map) data
    def value = data.get("enabled")
    setEnabled(value)
    if(value)
         updateSkin()
    else sendMessage('gui:set-skin-filter', data)
})
