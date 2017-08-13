// TODO: add a filter for morning
sunset = [red: 0.94, green: 0.82, blue: 1.0]
night = [red: 0.63, green: 0.78, blue: 0.82]

def updateSkin() {
    doDependingOnDaytime(
        night: { setSkinFilter(night) },
        evening: { setSkinFilter(sunset) }
    )
    sendMessage('core-utils:notify-after-delay', [delay: 3600000], { sender, tag, data ->
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

sendMessage('gui:setup-options-tab', [name: 'Skin Filter', msgTag: tagApply, controls: [
    [
        type: 'FloatSpinner', id: 'red', label: 'Red',
        value: 0.63, min: 0.0, max: 1.0, step: 0.01
    ],
    [
        type: 'FloatSpinner', id: 'green', label: 'Green',
        value: 0.78, min: 0.0, max: 1.0, step: 0.01
    ],
    [
        type: 'FloatSpinner', id: 'blue', label: 'Blue',
        value: 0.82, min: 0.0, max: 1.0, step: 0.01
    ],
    [
        type: 'FloatSpinner', id: 'opacity', label: 'Opacity',
        value: 1.0, min: 0.0, max: 1.0, step: 0.01
    ]
]])

addMessageListener(tagApply, { sender, tag, data ->
    sendMessage('gui:set-skin-filter', data)
})
