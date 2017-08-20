import java.text.SimpleDateFormat

Database.filename=getDataDirPath().resolve('database').toString()
database=new Database(this)

Localization.instance=this
Localization.load()

defaultSoundFolder=getPluginDirPath().resolve("sounds")
format = new SimpleDateFormat ('dd.MM.yyyy')

void setupEventsMenu(){
    dt = new Date()
    sendMessage( 'gui:setup-options-submenu',
        [ 'name': Localization.getString('shedule'),
          'msgTag': 'organizer:add-event',
          'controls': [
            [
                'type': 'ListBox',
                'id': 'events',
                'label': Localization.getString('sheduled'),
                'values': database.getListOfEntries(),
                'msgTag': 'organizer:selected-changed'
            ],[
                'type': 'Button',
                'msgTag': 'organizer:delete-selected',
                'value': Localization.getString('delete')
            ],[
                'type': 'Label',
                'label': Localization.getString('create-reminder')
            ],[
                'type': 'TextField',
                'id': 'name',
                'label': Localization.getString('name'),
                'value': Localization.getString('default-reminder')
            ],[
                'type': 'DatePicker',
                'id': 'date',
                'label': Localization.getString('date'),
                'format': format.toPattern(),
                'value': new Date().format( format.toPattern() )
            ],[
                'type': 'Spinner',
                'id': 'hour',
                'label': Localization.getString('hour'),
                'value': (Integer.parseInt(dt.format('H'))+1)%24,
                'min': 0,
                'max': 23,
                'step': 1
            ],[
                'type': 'Spinner',
                'id': 'minute',
                'label': Localization.getString('minute'),
                'value': 0,
                'min': 0,
                'max': 59,
                'step': 1
            ],[
                'type': 'CheckBox',
                'id': 'soundEnabled',
                'label': Localization.getString('enable-sound'),
                'value': false,
                'msgTag': 'organizer:check-sound'
            ],[
                'type': 'FileField',
                'id': 'sound',
                'label': Localization.getString('sound'),
                'value': Localization.getString('default'),
                'disabled': true,
                'initialDirectory':  defaultSoundFolder.toString(),
                'filters': [[
                    'description': Localization.getString('sound'),
                    'extensions': ['*.mp3', '*.wav', '*.aac', '*.ogg', '*.flac']
                ]]
            ]
          ]
        ]
    )
}
sendMessage( 'gui:setup-options-submenu',
        [ 'name': Localization.getString('timer'),
          'msgTag': 'organizer:add-timer',
          'controls': [
                  [
                          'type': 'TextField',
                          'id': 'name',
                          'label': Localization.getString('name'),
                          'value': Localization.getString('default-timer')
                  ],[
                          'type': 'Spinner',
                          'id': 'hour',
                          'label': Localization.getString('hour'),
                          'value': 0,
                          'min': 0,
                          'max': 100000,
                          'step': 1
                  ],[
                          'type': 'Spinner',
                          'id': 'minute',
                          'label': Localization.getString('minute'),
                          'value': 0,
                          'min': 0,
                          'max': 100000,
                          'step': 1
                  ],[
                          'type': 'Spinner',
                          'id': 'second',
                          'label': Localization.getString('second'),
                          'value': 0,
                          'min': 0,
                          'max': 100000,
                          'step': 1
                  ],[
                          'type': 'CheckBox',
                          'id': 'soundEnabled',
                          'label': Localization.getString('enable-sound'),
                          'value': false,
                          'msgTag': 'organizer:check-timer-sound'
                  ],[
                          'type': 'FileField',
                          'id': 'sound',
                          'label': Localization.getString('sound'),
                          'value': Localization.getString('default'),
                          'disabled': true,
                          'initialDirectory': defaultSoundFolder.toString(),
                          'filters': [[
                                              'description': Localization.getString('sound'),
                                              'extensions': ['*.mp3', '*.wav', '*.aac', '*.ogg', '*.flac']
                                      ]]
                  ]
          ]
        ]
)
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
            [ 'name': 'Scheduler',
              'controls': [[
                                   'id': 'sound',
                                   'disabled': !data.get("value")
                           ]]
            ])
})
addMessageListener('organizer:check-timer-sound', { sender, tag, data ->
    sendMessage( 'gui:update-options-submenu',
            [ 'name': 'Set Timer',
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
    println sender
    String name=data.get("name")
    if(name.length()==0){
        sendMessage( 'gui:show-notification',
                [ 'name': Localization.getString('error'),
                  'text': 'No name specified'
                ])
        return
    }
    Calendar calendar=Calendar.instance
    calendar.setTime(format.parse(data.get("date")))
    calendar.set(Calendar.HOUR_OF_DAY,data.get("hour"))
    calendar.set(Calendar.MINUTE,data.get("minute"))
    switch(database.addEventEntry(calendar, name, data.get("soundEnabled") ? data.get("sound") : null)){
        case 0:
            setupEventsMenu()
            break
        case 1:
            sendMessage( 'gui:show-notification',
                    [ 'name': Localization.getString('error'),
                      'text': Localization.getString('error.past')
                    ])
            break
    }
})
addMessageListener('organizer:add-timer', { sender, tag, data ->
    println sender
    String name=data.get("name")
    if(name.length()==0){
        sendMessage( 'gui:show-notification',
                [ 'name': Localization.getString('error'),
                  'text': Localization.getString('error.no-name')
                ])
        return
    }
    int delay=data.get("second")+data.get("minute")*60+data.get("hour")*3600
    database.addTimerEntry(delay, name, data.get("soundEnabled") ? data.get("sound") : null)
    setupEventsMenu()
})

setupEventsMenu()