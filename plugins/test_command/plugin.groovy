// здесь заполняете имя вашего плагина
pluginName = 'testCommand'

// здесь мы регистрируем нашу команду
sendMessage("core:add-command", [ tag: pluginName+':command' ])

// здесь выполняете любой код, который должна делать ваша команда
addMessageListener(pluginName+':command', { sender, tag, data ->
    text=((HashMap<String,Object>)data).getOrDefault('msgData','Что ты хочещь услышать, сахарочек?')
    sendMessage("DeskChan:say", text.toString())
})

// здесь мы связываем команду и событие
sendMessage("core:set-event-link",
    [
        eventName: 'speech:get',
        commandName: pluginName+':command',
        rule: 'скажи что нибудь'  // слова, которые должен произнести юзер
    ]
)