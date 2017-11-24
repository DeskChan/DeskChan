import info.deskchan.speech_command_system.PhraseComparison

// здесь заполняете имя вашего плагина
pluginName = 'chat_sugar'

// здесь мы регистрируем нашу команду
sendMessage("core:add-command", [ tag: pluginName+':magic-ball' ])
sendMessage("core:add-command", [ tag: pluginName+':choose' ])

ball_list = ['Бесспорно', 'Предрешено', 'Никаких сомнений', 'Определённо да', 'Можешь быть уверен в этом',
    'Мне кажется — «да»', 'Вероятнее всего', 'Хорошие перспективы', 'Знаки говорят — «да»', 'Да',
    'Пока не ясно, попробуй снова', 'Спроси позже', 'Лучше не рассказывать', 'Сейчас нельзя предсказать', 'Сконцентрируйся и спроси опять',
    'Даже не думай', 'Мой ответ — «нет»', 'По моим данным — «нет»', 'Перспективы не очень хорошие', 'Весьма сомнительно']

class Buffer {
    int counter = 0
    int size = 50
    def buffer

    Buffer(int size){
        this.size = size
        buffer = []
        for (int i = 0; i < size; i++)
            buffer.add([ "", "" ])
    }

    String get(String query){
        query = query.toLowerCase()
        for (int i = 0; i < size; i++)
            if(query.equals(buffer[i][0]))
                return buffer[i][1]
        return null
    }

    void add(String query, String answer){
        buffer[counter] = [query, answer]
        counter = (counter + 1) % size
    }
}

buffer = new Buffer(50)

// здесь выполняете любой код, который должна делать ваша команда
addMessageListener(pluginName+':magic-ball', { sender, tag, dat ->
    Map data = (Map) dat
    def text = data.get("text")
    if(text == null || text.size() == 0){
        sendMessage('DeskChan:say', 'Но ты же ничего не спросил!')
        return
    }
    ar = text
    text = ""
    for(String var : ar)
        text += var

    answer = buffer.get(text)
    if(answer != null){
        sendMessage("DeskChan:say", "Зачем ты снова меня это спрашиваешь? Я же ответила - "+answer)
        return
    }
    Random ran = new Random()
    i = ran.nextFloat()
    length = 1 / ball_list.size()
    i = (int) (i / length)
    sendMessage("DeskChan:say", ball_list[i])
    buffer.add(text, ball_list[i])
})

addMessageListener(pluginName+':choose', { sender, tag, dat ->
    Map data = (Map) dat
    String text = data.get("text")
    if(text == null || text.size() == 0){
        sendMessage('DeskChan:say', 'Но ты же ничего не спросил!')
        return
    }
    answer = buffer.get(text)
    if(answer != null){
        sendMessage("DeskChan:say", "Зачем ты снова меня это спрашиваешь? Я же ответила - "+answer)
        return
    }
    String[] choices = text.toLowerCase().split("(,|([^А-я]и[^А-я])|([^А-я]или[^А-я]))")
    println(choices)
    Random ran = new Random()
    i = ran.nextFloat()
    i = (int) (i * choices.length)
    answer = choices[i].trim()
    sendMessage("DeskChan:say", answer)
    buffer.add(text, answer)
})

// здесь мы связываем команду и событие
sendMessage("core:set-event-link",
        [
            eventName: 'speech:get',
            commandName: pluginName+':magic-ball',
            rule: 'посоветуй {text:list}'
        ]
)
sendMessage("core:set-event-link",
        [
            eventName: 'speech:get',
            commandName: pluginName+':choose',
            rule: 'выбери ?из {text:text}'
        ]
)