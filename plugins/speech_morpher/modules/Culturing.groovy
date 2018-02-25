class Culturing{
    def bridge
    void initialize(bridge){  this.bridge = bridge  }

    static def random = new Random(System.currentTimeMillis())
    static def replacementsList = [
        ['сейчас', 'щас', 'ас'],
        ['магазин', 'магаз'],
        ['что', 'чё', 'чо', 'шо'],
        ['спасибо', 'сенкс', 'спс', "пасяб"],
        ['тебе', 'те'],
        ['тебя', 'тя'],
        ['нормально', 'норм', 'нормас'],
        ['пожалуйста', 'плез', 'пжлст', 'пож'],
        ['компьютер', 'пк', 'пека', 'кампухтер', 'кудкудахтер', 'компилястер'],
        ['быстро', 'быро'],
        ['да', 'ага', "агась"],
        ['нет', 'неа', "неее"],
        ['привет', 'дарова', "йоу"],
        ['ничего', 'ничё'],
        ['люблю', 'лю'],
        ['хочешь', 'хош'],
        ['почему', 'чому'],
        ['смотри', 'смари'],
        ['надо', 'надо'],
        ['давай', 'го'],
        ['нельзя', 'низя'],
        ['зачем', 'нахера', 'нафига'],
        ['ладно', 'лады']
    ]
    static def obsceneInsertions = [
            "хехе", "кароч", "ёмаё",
            "ёпт", "бля", "нах",
            "блять", "нахуй", "ебать"
    ]
    static def marks = ".,!?"

    int currentCulture = 0

    void setPreset(Map preset){
        currentCulture = preset.getOrDefault("manner", 0)
    }

    Map morphPhrase(Map phrase){
        if (currentCulture >= 0) return phrase
        def text = phrase.get("text")
        def words = text.split(" ")
        def finalText = ""

        for(String word : words) {
            String symbols = ""

            for (int i=word.length()-1; i >= 0; i--)
                if (marks.contains(word[i]))
                    symbols += word[i]
                else break

            word = word.substring(0, word.length() - symbols.length())

            char firstLetter = word[0]
            for(def i : replacementsList) {
                if(i[0].equals(word.toLowerCase()) && -random.nextFloat()*currentCulture > 0.8) {
                    int pos = 1+random.nextInt(i.size()-1)
                    word = i[pos]
                    if (Character.isUpperCase(firstLetter))
                        word[0] = word[0].toUpperCase()
                    break
                }
            }
            finalText += " " + word + symbols
        }
        if (currentCulture < -1) {
            int pos = (-2 - currentCulture)*3
            finalText = bridge.insert(finalText, obsceneInsertions[pos..pos+3])
        }
        phrase.put("text", finalText)
        return phrase
    }

    String getName(Locale locale){
        switch (locale){
            case new Locale("ru"): return "Плохие манеры"
            case new Locale("en"): return "Bad manners"
        }
    }
}