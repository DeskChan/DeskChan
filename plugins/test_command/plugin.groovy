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

// здесь мы регистрируем нашу команду
sendMessage("core:add-command", [ tag: pluginName+':run' ])
sendMessage("core:add-command", [ tag: pluginName+':run-with-report' ])
sendMessage("core:add-command", [ tag: pluginName+':run-with-multiple-report' ])

// здесь выполняете любой код, который должна делать ваша команда
addMessageListener(pluginName+':run', { sender, tag, dat ->
    if(dat==null) return
    String line = ((Map)dat).get("msgData").toString()
    line.execute()
    sendMessage("DeskChan:say","Запущено")
})

addMessageListener(pluginName+':run-with-report', { sender, tag, dat ->
    if(dat==null) return
    String line = ((Map)dat).get("msgData").toString()
    sendMessage("DeskChan:say","Запущено, ожидаем")
    Process process = line.execute()
    Thread.start {
        process.waitFor()
        if(process.exitValue()==0)
            sendMessage("DeskChan:say","Ура, всё хорошо поработало!")
        else sendMessage("DeskChan:say","Ой, что-то не получилось. Код ошибки: "+process.exitValue())
        sendMessage("gui:show-notification",[text : process.text])
    }
})

addMessageListener(pluginName+':run-with-multiple-report', { sender, tag, dat ->
    if(dat==null) return
    String line = ((Map)dat).get("msgData").toString()
    sendMessage("DeskChan:say","Запущено, ожидаем")
    Process process = line.execute()
    Thread.start {
        def (output, error) = new StringWriter().with { o -> // For the output
            new StringWriter().with { e ->                     // For the error stream
                process.waitForProcessOutput(o, e)
                [o, e]*.toString()                             // Return them both
            }
        }
        process.waitFor()
        if (process.exitValue() == 0)
            sendMessage("DeskChan:say", "Ура, всё хорошо поработало!")
        else sendMessage("DeskChan:say", "Ой, что-то не получилось. Код ошибки: " + process.exitValue())
        sendMessage("gui:show-notification", [name: 'Standart output', text: output])
        sendMessage("gui:show-notification", [name: 'Error output', text: error])
    }
})

// здесь мы связываем команду и событие
sendMessage("core:set-event-link",
    [
        eventName: 'speech:get',
        commandName: pluginName+':run',
        rule: 'открой терминал',
        msgData: 'cmd.exe /c start'
    ]
)