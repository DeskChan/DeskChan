import org.json.JSONArray
import org.json.JSONObject

import java.nio.charset.StandardCharsets

class Database{
    DatabaseEntry addEventEntry(Calendar date, String name, String sound){
        def entry=new DatabaseEntry(0,date,name,sound)
        if(!checkCorrectTime(entry)) return null
        add(entry)
        save()
        notify(entry)
        return entry
    }
    DatabaseEntry addTimerEntry(int delay, String name, String sound){
        Calendar c=Calendar.instance
        c.add(Calendar.SECOND, delay)
        def entry=new DatabaseEntry(1,c,name,sound)
        if(!checkCorrectTime(entry)) return null
        add(entry)
        save()
        notify(entry)
        return entry
    }
    void addWatchEntry(Calendar date, String name, String sound){
        watcher=new DatabaseEntry(2,date,name,sound)
    }
    void add(DatabaseEntry entry){
        for(int i=0;i<entries.size();i++)
            if(entries.get(i).eventId.equals(entry.eventId)) {
                entries.set(i, entry)
                return
            }
        entries.add(entry)
    }
    static class DatabaseEntry{
        public static Object instance
        public static String defaultSound
        int type
        long time
        String eventId
        String soundPath
        int timerId
        DatabaseEntry(JSONObject obj){
            type = obj.getInt("type")
            time = obj.getLong("stamp")
            eventId = obj.getString("id")
            if(obj.has("soundPath"))
                soundPath = obj.getString("soundPath")
            else
                soundPath = null
        }
        DatabaseEntry(int type, Calendar date, String name, String sound){
            this.type = type
            time = date.getTimeInMillis()
            eventId = name
            soundPath = null
            if(sound != null){
                if(sound.equals(instance.getString('default')))
                    soundPath = defaultSound
                else if(sound.length()>0)
                    soundPath = sound
            }
        }
        JSONObject toJSON(){
            JSONObject obj=new JSONObject()
            obj.put("type",type)
            obj.put("stamp",time)
            obj.put("id",eventId)
            if(soundPath!=null) obj.put("soundPath", soundPath)
            return obj
        }
        String getTimeString(){
            return new Date(time).format('dd.MM.yyyy HH:mm')
        }
    }
    static boolean checkCorrectTime(DatabaseEntry entry){
        return entry.time-Calendar.instance.getTimeInMillis()>0
    }
    LinkedList entries
    DatabaseEntry watcher
    Object instance
    private static filename
    Database(Object instance){
        entries = new LinkedList<DatabaseEntry>()
        this.instance=instance
        DatabaseEntry.instance = instance
        load()
    }
    List getListOfEntries(){
        def list=[]
        for (int i = 0; i < entries.size(); i++) {
            list.add(entries[i].eventId+" [ "+entries[i].getTimeString()+" ]")
        }
        return list
    }
    void load(){
        def file = new File(filename)
        if(!file.exists() || !file.canRead()) return

        String text=file.getText('UTF-8')
        if(text.length()<3) return
        try {
            def content = new JSONArray(text)

            entries.clear()

            content.each {
                if (it != JSONObject.NULL) {
                    def entry = new DatabaseEntry(it)
                    if (checkCorrectTime(entry)) {
                        add(entry)
                        notify(entry)
                    }
                }
            }
        } catch(Exception e){
            instance.log("Error while loading database for organizer")
        }
    }
    void save(){
        def file = new File(filename)

        if(!file.exists()) file.createNewFile()

        JSONArray ar=new JSONArray()
        entries.each {
            ar.put(it.toJSON())
        }
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8)
        writer.write(ar.toString(2))
        writer.close()
    }
    void delete(List items) {
        for(int k = 0; k < items.size(); k++) {
            int i = items[k].lastIndexOf('[')
            def str = items[k].substring(0,i-1)
            for (i = 0; i < entries.size() && entries.size() > 0; i++) {
                DatabaseEntry entry = entries.getAt(i) as DatabaseEntry
                if (entry.eventId.equals(str)) {
                    instance.cancelTimer(entry.timerId)
                    entries.removeAt(i)
                    i--
                }
            }
        }
        save()
        instance.setupEventsMenu()
    }
    void print(){
        entries.each{
            println(it.toJSON())
        }
    }

    def notify(Database.DatabaseEntry entry){
        def delay = entry.time - Calendar.instance.getTimeInMillis()

        entry.timerId = instance.setTimer(delay, { sender, data ->
            instance.sendMessage('DeskChan:request-say',[ 'purpose': "ALARM", 'timeout': 20, 'priority': 10000, 'partible': false ])
            if(entry.soundPath!=null)
                instance.sendMessage('gui:play-sound',[ 'file': entry.soundPath, /*'count': 10*/ ])
            entries -= entry
            save()
            instance.setupEventsMenu()
        })
    }

}
