import java.text.SimpleDateFormat
import info.deskchan.core.ResponseListener

setResourceBundle("resources")

Database.filename=getDataDirPath().resolve('database').toString()
database=new Database(this)

instance=this

defaultSoundFolder=getPluginDirPath().resolve("sounds")
format = new SimpleDateFormat ('dd.MM.yyyy')
def properties = getProperties()
properties.load()

sendMessage("talk:add-plugin-phrases", getPluginDirPath().resolve('organizer.phrases'))


/* -- Menu setup -- */


sendMessage( 'gui:set-panel',
        [ 'name': getString('menu'),
          'type': getString('submenu'),
          'id':   'menu',
          'action': 'set',
          'controls': [
             [
                 'type': 'Button',
                 'dstPanel': 'organizer-shedule',
                 'value': getString('shedule')
             ],[
                  'type': 'Button',
                  'dstPanel': 'organizer-timer',
                  'value': getString('timer')
             ],[
                  'type': 'Button',
                  'dstPanel': 'organizer-stopwatch',
                  'value': getString('stopwatch')
             ],[
                  'type': 'Button',
                  'dstPanel': 'organizer-tomato',
                  'value': getString('tomato')
             ]
          ]
        ]
)

void setupEventsMenu(){
    dt = new Date()
    sendMessage( 'gui:set-panel',
        [ 'name': getString('shedule'),
          'type': 'window',
          'id':   'shedule',
          'action': 'set',
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
sendMessage( 'gui:set-panel',
        [ 'name': getString('timer'),
          'type': 'window',
          'id':   'timer',
          'action': 'set',
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

Database.DatabaseEntry.defaultSound = defaultSoundFolder.resolve("communication-channel.mp3")

addMessageListener('organizer:check-sound', { sender, tag, data ->
    sendMessage( 'gui:set-panel',
            [ 'name': getString('shedule'),
              'id':   'shedule',
              'action': 'update',
              'controls': [[
                                   'id': 'sound',
                                   'disabled': !data
                           ]]
            ])
})
addMessageListener('organizer:check-timer-sound', { sender, tag, data ->
    sendMessage( 'gui:set-panel',
            [ 'name': getString('timer'),
              'id':   'timer',
              'action': 'update',
              'controls': [[
                                   'id': 'sound',
                                   'disabled': !data
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


/* -- Events -- */


sendMessage('core:add-command', [ tag: 'organizer:add-event' ])
sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: 'organizer:add-event',
        rule: 'поставь будильник {datetime:DateTime}'
])

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


/* -- Timers -- */


sendMessage('core:add-command', [ tag: 'organizer:add-timer' ])
sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: 'organizer:add-timer',
        rule: 'поставь таймер {delay:RelativeDateTime}'
])

addMessageListener('organizer:add-timer', { sender, tag, data ->
    String name = data.get("name")
    if(name == null || name.length() == 0)
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


/* -- Stopwatch -- */


sendMessage('core:add-command', [ tag: 'organizer:open-stopwatch' ])
sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: 'organizer:open-stopwatch',
        rule: '?(открой|запусти) секундомер'
])

sendMessage( 'gui:set-panel',
        [ 'name': getString('stopwatch'),
          'type': 'window',
          'id':   'stopwatch',
          'action': 'set',
          'controls': [
                  [
                          'type': 'Label',
                          'id': 'time',
                          'font': 'Arial, 24',
                          'align': 'center',
                          'value': '00:00:00',
                          'width': 200
                  ],[
                          'type': 'Label',
                          'id': 'seconds',
                          'font': 'Arial, 24',
                          'align': 'center',
                          'value': '0 '+getString('seconds'),
                          'width': 200
                  ],[
                          'elements': [
                                  [
                                          'type': 'Button',
                                          'id': 'button1',
                                          'value': getString('start'),
                                          'msgTag': 'organizer:start-stopwatch',
                                  ],[
                                          'type': 'Button',
                                          'id': 'button2',
                                          'value': getString('pause'),
                                          'msgTag': 'organizer:pause-stopwatch',
                                  ]
                          ]
                  ]
          ]
        ]
)

addMessageListener('organizer:open-stopwatch', { sender, tag, data ->
    sendMessage('gui:set-panel', [
            'name': getString('stopwatch'),
            'id':   'stopwatch',
            'action': 'show'
            ]
    )
})

def updateMenu(){
    minutes = (int) (seconds / 60)
    hours = (int) (seconds / 3600)
    sec = seconds - minutes*60 - hours*3600
    sendMessage( 'gui:set-panel',
            [ 'name': getString('stopwatch'),
              'id':   'stopwatch',
              'action': 'update',
              'controls': [
                      [  'id': 'time',    'value': sprintf('%02d:%02d:%02d', [hours, minutes, sec])  ],
                      [  'id': 'seconds', 'value': seconds + " " + getString('seconds')  ]
              ]
            ]
    )
}
timerId = -1
seconds = 0

def tick(){
    seconds++
    updateMenu()
}

addMessageListener('organizer:start-stopwatch', { sender, tag, data ->
    seconds = 0
    if (timerId >= 0) cancelTimer(timerId)
    timerId = setTimer(1000, -1, { s, d -> tick()})
    sendMessage( 'gui:set-panel',
            [ 'name': getString('stopwatch'),
              'id':   'stopwatch',
              'action': 'update',
              'controls': [
                      [  'id': 'time',    'value': '00:00:00'  ],
                      [  'id': 'seconds', 'value': "0 " + getString('seconds')  ],
                      [  'id': 'button1', 'value': getString('reset')  ],
                      [  'id': 'button2', 'value': getString('pause')  ]
              ]
            ]
    )
})

addMessageListener('organizer:pause-stopwatch', { sender, tag, data ->
    def name
    if (timerId >= 0){
        cancelTimer(timerId)
        name = getString('start')
        timerId = -1
    } else {
        timerId = setTimer(1000, -1, { s, d -> tick()})
        name = getString('pause')
    }
    sendMessage( 'gui:set-panel',
            [ 'name': getString('stopwatch'),
              'id':   'stopwatch',
              'action': 'update',
              'controls': [
                      [  'id': 'button1', 'value': getString('reset')  ],
                      [  'id': 'button2', 'value': name  ]
              ]
            ]
    )
})


/* -- Tomatos -- */


tomatoActive = false
tomatoId = -1
tomatoTimerId = -1
tomatoTimer = 0
tomatoPause = true
sendMessage( 'gui:set-panel',
        [ 'name': getString('tomato'),
          'type': 'panel',
          'id':   'tomato',
          'action': 'set',
          'msgTag': 'organizer:tomato-save-options',
          'controls': [
                  [
                      'type': 'IntSpinner',
                      'id': 'tomato-time',
                      'value': properties.getInteger('tomato-minutes', 30),
                      'label': getString('tomato-time'),
                      'max': 10000
                  ],[
                      'type': 'IntSpinner',
                      'id': 'tomato-pause',
                      'value': properties.getInteger('tomato-pause-minutes', 15),
                      'label': getString('tomato-pause'),
                      'max': 10000
                  ],[
                      'type': 'Label',
                      'id': 'active',
                      'value': getString('not-active'),
                  ],[
                      'type': 'Button',
                      'id': 'start',
                      'msgTag': 'organizer:toggle-tomato',
                      'value': getString(tomatoActive ? 'stop' : 'start'),
                  ]
          ]
        ]
)

addMessageListener('organizer:tomato-save-options', { sender, tag, data ->
    int time = properties.getInteger('tomato-minutes', 30)
    int pause = properties.getInteger('tomato-pause-minutes', 15)
    properties.put('tomato-minutes', data.get('tomato-time'))
    properties.put('tomato-pause-minutes', data.get('tomato-pause'))
    if (tomatoActive && ((!tomatoPause && time != data.get('tomato-time')) || (tomatoPause && pause != data.get('tomato-pause')))) {
        cancelTimer(tomatoId)
        cancelTimer(tomatoTimerId)
        tomatoActive = false
        sendMessage('gui:set-panel',
             [
                 'name'    : getString('tomato'),
                 'id'      : 'tomato',
                 'action'  : 'update',
                 'controls': [
                         ['id': 'active', 'value': getString('not-active')],
                         ['id': 'start',  'value': getString('start')]
                 ]
             ]
        )
    }
    properties.save()
})

tomatoListener = new ResponseListener(){
    void handle(String sender, Object data){
        tomatoHandle()
    }
}

def tomatoHandle(){
    tomatoPause = !tomatoPause
    tomatoTimer = Calendar.instance.timeInMillis
    sendMessage("DeskChan:request-say", tomatoPause ? "RELAX" : "DO_WORK")
    sendMessage("gui:show-notification", [
        'name': getString('tomato'),
        'text': getString( tomatoPause ? 'tomato-relax-alert' : 'tomato-start-alert' )
    ])
    tomatoId = setTimer( 60000 *
            (tomatoPause ?
                    properties.getInteger('tomato-pause-minutes', 15) :
                    properties.getInteger('tomato-minutes', 30)), 1, tomatoListener)
}

addMessageListener('organizer:toggle-tomato', { sender, tag, data ->
    tomatoActive = !tomatoActive
    def active
    if (tomatoActive){
        tomatoTimerId = setTimer(1000, -1, {s, d ->
            time = (Calendar.instance.timeInMillis - tomatoTimer)/1000
            int tm = time / 60
            int ts = time - tm * 60
            sendMessage( 'gui:set-panel',
                    [ 'name': getString('tomato'),
                      'id':   'tomato',
                      'action': 'update',
                      'controls': [
                        [  'id': 'active',
                           'value': getString('timer-active') + " " + tm + " min " + ts + " sec. " +
                                    getString( 'timer-state-' + (tomatoPause ? 'relax' : 'work') )]
                      ]
                    ])
        })
        active = [  'id': 'active', 'value': getString('timer-active') + " 0 min 0 sec"  ]
        tomatoHandle()
    } else {
        cancelTimer(tomatoId)
        cancelTimer(tomatoTimerId)
        tomatoTimer = 0
        tomatoPause = true
        active = [  'id': 'active', 'value': getString('not-active') ]
    }
    sendMessage( 'gui:set-panel',
            [ 'name': getString('tomato'),
              'id':   'tomato',
              'action': 'update',
              'controls': [
                 [  'id': 'start',  'value': getString( tomatoActive ? 'stop' : 'start' )  ],
                 active
              ]
            ]
    )
})


setupEventsMenu()