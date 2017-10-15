// здесь заполняете имя вашего плагина
pluginName = 'terminal'

// здесь мы регистрируем нашу команду
sendMessage("core:add-command", [ tag: pluginName+':command' ])

// здесь выполняете любой код, который должна делать ваша команда
addMessageListener(pluginName+':command', { sender, tag, dat ->
    HashMap<String,Object> data = dat
    if(data.containsKey('value'))
        sendMessage('DeskChan:say', data.get('value').toString())
    else {
        sendMessage('DeskChan:say','Что ты хочешь услышать, сахарочек?')
        sendMessage('DeskChan:request-user-speech',null, { s, d ->
            data = d
            sendMessage('DeskChan:say', data.get('value').toString())
        })
    }

})

// здесь мы связываем команду и событие
sendMessage("core:set-event-link",
        [
                eventName: 'speech:get',
                commandName: pluginName+':command',
                rule: 'скажи что нибудь'  // слова, которые должен произнести юзер
        ]
)