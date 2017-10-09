import java.text.SimpleDateFormat

setResourceBundle("resources")

Database.filename=getDataDirPath().resolve('database').toString()
database=new Database(this)

instance=this

defaultSoundFolder=getPluginDirPath().resolve("sounds")
format = new SimpleDateFormat ('dd.MM.yyyy')

void setupEventsMenu(){
    dt = new Date()
    sendMessage( 'gui:setup-options-submenu',
        [ 'name': getString('shedule'),
          'msgTag': 'organizer:add-event',
          'controls': [
            [
                'type': 'ListBox',
                'id': 'events',
                'label': getString('sheduled'),
                'values': database.getListOfEntries(),
                'msgTag': 'organizer:selected-changed'
            ],[
                'type': 'Button',
                'msgTag': 'organizer:delete-selected',
                'value': getString('delete')
            ],[
                'type': 'Label',
                'label': getString('create-reminder')
            ],[
                'type': 'TextField',
                'id': 'name',
                'label': getString('name'),
                'value': getString('default-reminder')
            ],[
                'type': 'DatePicker',
                'id': 'date',
                'label': getString('date'),
                'format': format.toPattern(),
                'value': new Date().format( format.toPattern() )
            ],[
                'type': 'Spinner',
                'id': 'hour',
                'label': getString('hour'),
                'value': (Integer.parseInt(dt.format('H'))+1)%24,
                'min': 0,
                'max': 23,
                'step': 1
            ],[
                'type': 'Spinner',
                'id': 'minute',
                'label': getString('minute'),
                'value': 0,
                'min': 0,
                'max': 59,
                'step': 1
            ],[
                'type': 'CheckBox',
                'id': 'soundEnabled',
                'label': getString('enable-sound'),
                'value': false,
                'msgTag': 'organizer:check-sound'
            ],[
                'type': 'FileField',
                'id': 'sound',
                'label': getString('sound'),
                'value': getString('default'),
                'disabled': true,
                'initialDirectory':  defaultSoundFolder.toString(),
                'filters': [[
                    'description': getString('sound'),
                    'extensions': ['*.mp3', '*.wav', '*.aac', '*.ogg', '*.flac']
                ]]
            ]
          ]
        ]
    )
}
sendMessage( 'gui:setup-options-submenu',
        [ 'name': getString('timer'),
          'msgTag': 'organizer:add-timer',
          'controls': [
                  [
                          'type': 'TextField',
                          'id': 'name',
                          'label': getString('name'),
                          'value': getString('default-timer')
                  ],[
                          'type': 'Spinner',
                          'id': 'hour',
                          'label': getString('hour'),
                          'value': 0,
                          'min': 0,
                          'max': 100000,
                          'step': 1
                  ],[
                          'type': 'Spinner',
                          'id': 'minute',
                          'label': getString('minute'),
                          'value': 0,
                          'min': 0,
                          'max': 100000,
                          'step': 1
                  ],[
                          'type': 'Spinner',
                          'id': 'second',
                          'label': getString('second'),
                          'value': 0,
                          'min': 0,
                          'max': 100000,
                          'step': 1
                  ],[
                          'type': 'CheckBox',
                          'id': 'soundEnabled',
                          'label': getString('enable-sound'),
                          'value': false,
                          'msgTag': 'organizer:check-timer-sound'
                  ],[
                          'type': 'FileField',
                          'id': 'sound',
                          'label': getString('sound'),
                          'value': getString('default'),
                          'disabled': true,
                          'initialDirectory': defaultSoundFolder.toString(),
                          'filters': [[
                                              'description': getString('sound'),
                                              'extensions': ['*.mp3', '*.wav', '*.aac', '*.ogg', '*.flac']
                                      ]]
                  ]
          ]
        ]
)
sendMessage('core:add-command', [ tag: 'organizer:add-event' ])
sendMessage('core:add-command', [ tag: 'organizer:add-timer' ])

sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: 'organizer:add-event',
        rule: 'поставь будильник {datetime:DateTime}'
])
sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: 'organizer:add-timer',
        rule: 'поставь таймер {delay:RelativeDateTime}'
])
/*sendMessage( 'gui:setup-options-submenu',
        [ 'name': 'Watch',
          'controls': [
                  [
                          'type': 'TextField',
                          'id': 'time',
                          'value': '00:00:00.000'
                  ],[
                          'type': 'Button',
                          'id': 'start',
                          'label': 'Start',
                          'msgTag': 'organizer:start'
                  ]
          ]
        ]
)*/  /// MAYBE I'LL DO IT SOMEDAY
Database.DatabaseEntry.defaultSound = defaultSoundFolder.resolve("communication-channel.mp3")

addMessageListener('organizer:check-sound', { sender, tag, data ->
    sendMessage( 'gui:update-options-submenu',
            [ 'name': getString('shedule'),
              'controls': [[
                                   'id': 'sound',
                                   'disabled': !data.get("value")
                           ]]
            ])
})
addMessageListener('organizer:check-timer-sound', { sender, tag, data ->
    sendMessage( 'gui:update-options-submenu',
            [ 'name': getString('timer'),
              'controls': [[
                                   'id': 'sound',
                                   'disabled': !data.get("value")
                           ]]
            ])
})
def selected
addMessageListener('organizer:selected-changed', { sender, tag, data ->
    selected = data.get("value")
})
addMessageListener('organizer:delete-selected', { sender, tag, data ->
    database.delete(selected)
    setupEventsMenu()
})
addMessageListener('organizer:add-event', { sender, tag, data ->
    String name=data.get("name")
    if(name==null || name.length()==0)
        name = getString('default-reminder')

    Calendar calendar=Calendar.instance
    if(data.get("datetime")!=null)
        calendar.setTimeInMillis(data.get("datetime"))
    else {
        calendar.setTime(format.parse(data.get("date")))
        calendar.set(Calendar.HOUR_OF_DAY, data.get("hour"))
        calendar.set(Calendar.MINUTE, data.get("minute"))
    }

    entry = database.addEventEntry(calendar, name, data.get("soundEnabled") ? data.get("sound") : null)
    if(entry != null)
        sendMessage("DeskChan:say","Выполнено! Поставила на "+entry.getTimeString()+". Готовься!")
    else sendMessage("DeskChan:say", getString('error.past'))
    setupEventsMenu()
})
addMessageListener('organizer:add-timer', { sender, tag, data ->
    String name = data.get("name")
    if(name==null || name.length()==0)
        name = getString('default-timer')

    int delay
    if(data.get("delay") != null) {
        delay = data.get("delay")/1000
    } else delay = data.get("second")+data.get("minute")*60+data.get("hour")*3600

    entry = database.addTimerEntry(delay, name, data.get("soundEnabled") ? data.get("sound") : null)
    if(entry != null)
         sendMessage("DeskChan:say","Выполнено! Поставила на "+entry.getTimeString()+". Готовься!")
    else sendMessage("DeskChan:say", getString('error.past'))
    setupEventsMenu()
})

setupEventsMenu()