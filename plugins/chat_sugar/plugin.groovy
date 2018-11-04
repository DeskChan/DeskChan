import java.text.SimpleDateFormat

// здесь заполняете имя вашего плагина
pluginName = 'chat_sugar'

// здесь мы регистрируем нашу команду
sendMessage("core:add-command", [ tag: pluginName+':magic-ball' ])
sendMessage("core:add-command", [ tag: pluginName+':choose' ])
sendMessage("core:add-command", [ tag: pluginName+':random-number' ])
sendMessage("core:add-command", [ tag: pluginName+':dice' ])
sendMessage("core:add-command", [ tag: pluginName+':date' ])
sendMessage("core:add-command", [ tag: pluginName+':time' ])
sendMessage("core:add-command", [ tag: pluginName+':roulette' ])

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

addMessageListener(pluginName+':magic-ball', { sender, tag, dat ->
    Map data = (Map) dat
    def text = data.get("msgData")
    if(text == null || text.size() == 0){
        sendMessage('DeskChan:request-say', 'Но ты же ничего не спросил!')
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
    String text = data.get("msgData")
    if(text == null || text.size() == 0){
        sendMessage('DeskChan:say', 'Но ты же ничего не спросил!')
        return
    }
    answer = buffer.get(text)
    if(answer != null){
        sendMessage("DeskChan:say", "Зачем ты снова меня это спрашиваешь? Я же ответила - "+answer)
        return
    }
    String[] choices = text.toLowerCase().replace('?','').split("(,|([^А-я]и[^А-я])|([^А-я]или[^А-я]))")
    Random ran = new Random()
    i = ran.nextFloat()
    i = (int) (i * choices.length)
    answer = choices[i].trim()
    sendMessage("DeskChan:say", answer)
    buffer.add(text, answer)
})

addMessageListener(pluginName+':dice', { sender, tag, dat ->
    Map data = (Map) dat
    Integer to = data.get("to")
    if(to == null || to == 0)
        to = 6

    sendMessage("DeskChan:say", 1 + new Random().nextInt(to))
})

addMessageListener(pluginName+':random-number', { sender, tag, dat ->
    println(dat)
    Map data = (Map) dat
    Integer to = data.get("to")
    if(to == null || to == 0){
        sendMessage('DeskChan:say', 'Я не поняла, что ты написал, так что вот тебе: ' + new Random().nextFloat())
        return
    }
    Integer from = data.get("from")
    if(from == null) from = 0
    if (to < from) {
        def a = to
        to = from
        from = a
    }
    sendMessage("DeskChan:say", from + new Random().nextInt(to - from + 1))
})

def months = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'августа', 'сентября', 'октября', 'ноября', 'декабря']
def weekdays = ['воскресенье', 'понедельник', 'вторник', 'среда', 'четверг', 'пятница', 'суббота']

addMessageListener(pluginName+':date', { sender, tag, dat ->
    Map data = (Map) dat
    Long date = data.get("date")
    Calendar calendar = Calendar.instance

    if(date != null) {
        calendar.setTimeInMillis(date)
    } else {
        date = calendar.getTimeInMillis()
    }

    String result = weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1] + ", " +
                    calendar.get(Calendar.DAY_OF_MONTH) + " " +
                    months[calendar.get(Calendar.MONTH)-1] + ", " +
                    calendar.get(Calendar.YEAR)

    Calendar day = Calendar.instance
    if (day.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)){
        sendMessage("DeskChan:say", "Сегодня " + result)
        return
    }
    day.add(Calendar.DAY_OF_YEAR, 1)
    if (day.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)){
        sendMessage("DeskChan:say", "Завтра будет " + result)
        return
    }
    day.add(Calendar.DAY_OF_YEAR, 1)
    if (day.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)){
        sendMessage("DeskChan:say", "Послезавтра будет " + result)
        return
    }
    day.add(Calendar.DAY_OF_YEAR, -3)
    if (day.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)){
        sendMessage("DeskChan:say", "Вчера был " + result)
        return
    }
    day.add(Calendar.DAY_OF_YEAR, -1)
    if (day.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)){
        sendMessage("DeskChan:say", "Позавчера был " + result)
        return
    }
    if (day.getTimeInMillis() < date)
        sendMessage("DeskChan:say", "Это был " + result)
    else
        sendMessage("DeskChan:say", "Это будет " + result)
})

addMessageListener(pluginName+':time', { sender, tag, dat ->
    Map data = (Map) dat
    Long date = data.get("time")
    Calendar calendar = Calendar.instance

    if(date != null) {
        if (calendar.getTimeInMillis() < date)
            sendMessage("DeskChan:say", "Это будет " + calendar.cformat("HH:mm:ss"))
        else
            sendMessage("DeskChan:say", "Это был " + calendar.format("HH:mm:ss"))
    } else {
        sendMessage("DeskChan:say", "Сейчас " + calendar.format("HH:mm:ss"))
    }
})

addMessageListener(pluginName+':roulette', { sender, tag, dat ->
    if (new Random().nextInt(6) == 0){
        sendMessage("DeskChan:say", "Упс. Ты мёртв.")
    } else {
        sendMessage("DeskChan:say", "На этот раз тебе повезло. На этот раз.")
    }
})
// здесь мы связываем команду и событие
sendMessage("core:set-event-link", [
        eventName: 'speech:get',
        commandName: pluginName+':magic-ball',
        rule: 'ответь {text:list}'
])
sendMessage("core:set-event-link", [
        eventName: 'speech:get',
        commandName: pluginName+':choose',
        rule: '(выбери ?из)|(или)'
])
sendMessage("core:set-event-link", [
        eventName: 'speech:get',
        commandName: pluginName+':random-number',
        rule: '(случайное|сгенерируй) число ?от {from:Integer} ?до {to:Integer}'
])
sendMessage("core:set-event-link", [
        eventName: 'speech:get',
        commandName: pluginName+':dice',
        rule: 'кубик {to:Integer}'
])
sendMessage("core:set-event-link", [
        eventName: 'speech:get',
        commandName: pluginName+':date',
        rule: 'какой (число|день) {date:DateTime}'
])
sendMessage("core:set-event-link", [
        eventName: 'speech:get',
        commandName: pluginName+':time',
        rule: '(сколько|какое) (время|времени) {time:DateTime}'
])
sendMessage("core:set-event-link", [
        eventName: 'speech:get',
        commandName: pluginName+':roulette',
        rule: 'рулетка'
])