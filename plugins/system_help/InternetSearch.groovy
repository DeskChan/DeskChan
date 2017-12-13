import info.deskchan.speech_command_system.PhraseComparison

class InternetSearch {
    static String getLink(type, query) {
        query = query.replaceAll(' ', '%20')
        switch (type) {
            case "google":
                query = "https://www.google.com/search?q=" + query
                break
            case "yandex":
                query = "https://www.yandex.ru/?text=" + query
                break
            case "youtube":
                query = "https://www.youtube.com/results?search_query=" + query
                break
            case "wiki":
                query = "https://ru.wikipedia.org/w/index.php?search=" + query
                break
            case "ali":
                query = "https://ru.aliexpress.com/wholesale?SearchText=" + query
                break
            case "duck":
            default:
                query = "https://duckduckgo.com/?q=" + query
                break
        }
        return query
    }

    static String[][] variants = [[ 'google', 'гугл'   ],
                           [ 'yandex', 'яндекс' ],
                           [ 'youtube', 'ютуб', 'тытруба' ],
                           [ 'wiki', 'wikipedia', 'вики', 'википедия' ],
                           [ 'duck', 'duckduckgo', 'утка' ],
                           [ 'ali', 'aliexpress', 'али', 'алиэкспресс' ]]

    static def data
    static void initialize(pluginName, data) {
        this.data = data
        def instance = data.instance
        instance.sendMessage("core:add-command", [tag: pluginName + ':internet-search'])

        instance.addMessageListener(pluginName + ':internet-search', { sender, tag, dat ->
            List<String> words = ((Map) dat).getOrDefault("query", new ArrayList())
            String text = ((Map) dat).getOrDefault("text", "")
            if (text.contains('загугли')){
                String query = ""
                for (String w : words){
                    query += w + " "
                }
                instance.sendMessage(pluginName + ':open-link', getLink(variants[0][0], query))
                return
            }
            for (String word : words) {
                for (String[] variant : variants) {
                    for (String item : variant) {
                        if (PhraseComparison.relative(item, word) > 0.74) {
                            String query = ""
                            for (String w : words){
                                if (w != word) query += w + " "
                            }
                            instance.sendMessage(pluginName + ':open-link', getLink(variant[0], query))
                            return
                        }
                    }
                }
            }
            String query = ""
            for (String w : words){
                query += w + " "
            }
            instance.sendMessage(pluginName + ':open-link', getLink(null, query))
        })

        instance.sendMessage("core:set-event-link", [
                eventName  : 'speech:get',
                commandName: pluginName + ':internet-search',
                rule       : '{query:List} (найди|загугли) ?(в|на) ?(интернет|сети)'
        ])
    }
}
