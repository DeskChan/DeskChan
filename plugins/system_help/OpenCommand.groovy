import jdk.nashorn.internal.runtime.JSONListAdapter

import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

class OpenCommand{
    static String filename
    static class LinkEntry{
        public enum Types { FILE, PATH, WEBLINK, SCRIPT, COMMAND }
        def keywords
        String value
        Types type
        LinkEntry(String value, Types type, keywords){
            this.value = value
            this.type = type
            this.keywords = keywords
        }
        LinkEntry(JSONObject obj){
            value = obj.getString("value")
            keywords = obj.getJSONArray("keywords").toList()
            try {
                type = Types.valueOf(obj.getString("type"))
            } catch (Exception e){
                type = Types.COMMAND
            }
        }
        String wrap(String val){
            if(!val.contains(' '))
                return val
            if(val[0] !='"') val = '"'+val
            if(val[-1]!='"') val = val+'"'
            return val
        }
        JSONObject toJSON(){
            JSONObject obj=new JSONObject()
            obj.put("type", type)
            obj.put("value", value)
            JSONArray kw = new JSONArray()
            keywords.each {
                kw.put(it)
            }
            obj.put("keywords", kw)
            return obj
        }
        void open(){
            switch(data.system){
                case PluginData.OS.WINDOWS:
                    switch(type){
                        case Types.COMMAND:
                            value.execute()
                            break
                        default:
                            ('cmd /c start '+wrap(value)).execute()
                            break
                    }
                    break
                case PluginData.OS.UNIX:
                    switch(type){
                        case Types.COMMAND:
                            value.execute()
                            break
                        default:
                            ('xdg-open '+wrap(value)).execute()
                            break
                    }
                    break
                case PluginData.OS.MACOS:
                    switch(type){
                        case Types.COMMAND:
                            value.execute()
                            break
                        default:
                            ('open '+wrap(value)).execute()
                            break
                    }
                    break
            }

        }
    }
    static ArrayList<LinkEntry> entries = new ArrayList<>()
    static void load() {
        def file = new File(filename)
        def text = ''
        if (!file.exists() || !file.canRead() || (text = file.getText('UTF-8')).length()<3){
            createDefault()
            return
        }
        try {
            def content = new JSONArray(text)

            entries.clear()
            content.each {
                if (it != JSONObject.NULL) {
                    entries.add(new LinkEntry(it))
                }
            }
        } catch (Exception e) {
            data.instance.log("Error while loading links list, creating new")
            data.instance.log(e)
            createDefault()
        }
    }
    static void save() {
        def file = new File(filename)

        if (!file.exists()) file.createNewFile()

        JSONArray ar = new JSONArray()
        entries.each {
            ar.put(it.toJSON())
        }
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8)
        writer.write(ar.toString(2))
        writer.close()
    }
    static def data
    static void initialize(pluginName, data) {
        this.data = data
        def instance = data.instance
        filename = data.dataPath.resolve('links').toString()
        load()
        instance.sendMessage("core:add-command", [tag: pluginName + ':run'])
        instance.sendMessage("core:add-command", [tag: pluginName + ':open'])

        instance.addMessageListener(pluginName + ':open', { sender, tag, d ->
            Map m = d
            List text = (List) m.get('text')
            if(text==null){
                instance.sendMessage('talk:request', 'CLARIFY')
                instance.sendMessage('DeskChan:request-user-speech', null) { s, d2 ->
                    text = (List) d2['value'].split(' ')
                }
            }

            for(int i = 0; i<text.size(); i++){
                for(int j = 0; j<entries.size(); j++){
                    entries.get(j).keywords.each{
                        if(text[i].equals(it)){
                            entries.get(j).open()
                            return
                        }
                    }
                }
            }
            instance.sendMessage('DeskChan:say', 'Ой, а я не знаю, как это открыть...')
        })

        instance.sendMessage("core:set-event-link",
                [
                        eventName  : 'speech:get',
                        commandName: pluginName + ':open',
                        rule       : 'открой {text:List}'
                ]
        )

        instance.sendMessage("core:add-command", [ tag: pluginName+':run' ])
        instance.sendMessage("core:add-command", [ tag: pluginName+':run-with-report' ])
        instance.sendMessage("core:add-command", [ tag: pluginName+':run-with-multiple-report' ])

        instance.addMessageListener(pluginName+':run', { sender, tag, dat ->
            if(dat==null) return
            String line = ((Map)dat).get("msgData").toString()
            line.execute()
            instance.sendMessage("DeskChan:say","Запущено")
        })

        instance.addMessageListener(pluginName+':run-with-report', { sender, tag, dat ->
            if(dat==null) return
            String line = ((Map)dat).get("msgData").toString()
            instance.sendMessage("DeskChan:say","Запущено, ожидаем")
            Process process = line.execute()
            Thread.start {
                process.waitFor()
                if(process.exitValue()==0)
                    instance.sendMessage("DeskChan:say","Ура, всё хорошо поработало!")
                else instance.sendMessage("DeskChan:say","Ой, что-то не получилось. Код ошибки: "+process.exitValue())
                instance.sendMessage("gui:show-notification",[text : process.text])
            }
        })

        instance.addMessageListener(pluginName+':run-with-multiple-report', { sender, tag, dat ->
            if(dat==null) return
            String line = ((Map)dat).get("msgData").toString()
            instance.sendMessage("DeskChan:say","Запущено, ожидаем")
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
                    instance.sendMessage("DeskChan:say", "Ура, всё хорошо поработало!")
                else instance.sendMessage("DeskChan:say", "Ой, что-то не получилось. Код ошибки: " + process.exitValue())
                instance.sendMessage("gui:show-notification", [name: 'Standart output', text: output])
                instance.sendMessage("gui:show-notification", [name: 'Error output', text: error])
            }
        })
    }
    static void createDefault(){
        switch(data.system){
            case PluginData.OS.WINDOWS:
                entries.add(new LinkEntry("cmd.exe /c start", LinkEntry.Types.COMMAND, ['командная строка', 'терминал']))
                entries.add(new LinkEntry("notepad", LinkEntry.Types.COMMAND, ['блокнот', 'notepad']))
                break
            case PluginData.OS.UNIX:

                break
            case PluginData.OS.MACOS:

                break
        }

        entries.add(new LinkEntry("https://google.com", LinkEntry.Types.WEBLINK, ['гугл', 'google']))
        entries.add(new LinkEntry("https://yandex.ru", LinkEntry.Types.WEBLINK, ['яндекс', 'yandex']))
        entries.add(new LinkEntry("https://2ch.hk", LinkEntry.Types.WEBLINK, ['двач', '2ch']))

        def file = new File(filename)
        file.createNewFile()

        JSONArray ar = new JSONArray()
        entries.each {
            ar.put(it.toJSON())
        }
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8)
        writer.write(ar.toString(2))
        writer.close()
    }
}