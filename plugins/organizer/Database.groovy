import org.json.JSONArray
import org.json.JSONObject

import java.nio.charset.StandardCharsets

// This class contains all events entries that was created by organizer plugin
// It can save events to file but it happens only on plugin unloading
class Database{
    
    // Event - will happen in the future ON the exact time
    DatabaseEntry addEventEntry(Calendar date, String name, String sound){
        def entry = new DatabaseEntry(DatabaseEntry.Type.EVENT, date, name, sound)
        if(!isTimeInFuture(entry)) return null
        add(entry)
        save()
        notify(entry)
        return entry
    }
    
    // Timer - will happen in the future AFTER the exact time
    DatabaseEntry addTimerEntry(int delay, String name, String sound){
        Calendar c = Calendar.instance
        c.add(Calendar.SECOND, delay)
        def entry = new DatabaseEntry(DatabaseEntry.Type.TIMER, c, name, sound)
        if(!isTimeInFuture(entry)) return null
        add(entry)
        save()
        notify(entry)
        return entry
    }
    
    // Watch - only counts time, without notifications
    void addWatchEntry(Calendar date, String name, String sound){
        watcher = new DatabaseEntry(DatabaseEntry.Type.WATCH, date, name, sound)
    }
    
    // Adds entry to database with rewriting
    void add(DatabaseEntry entry){
        for(int i=0;i<entries.size();i++)
            if(entries.get(i).eventId.equals(entry.eventId)) {
                entries.set(i, entry)
                return
            }
        entries.add(entry)
    }
    
    // Data structure containing all information about event
    static class DatabaseEntry{
        
        // Plugin Proxy instance
        public static Object instance
        
        // Default soundfile path
        public static String defaultSound
        
        enum Type {
            EVENT, TIMER, WATCH

            static int toInt(Type val){
                switch (val){
                    case EVENT: return 0
                    case TIMER: return 1
                    case WATCH: return 2
                }
            }

            static Type fromInt(int val){
                switch (val){
                    case 0: return EVENT
                    case 1: return TIMER
                    case 2: return WATCH
                }
            }
        }
        Type type
        
        // Timestamp of event
        long time
        
        // Event string representation for GUI interface
        String eventId
        
        // Path to sound file
        String soundPath
        
        // If it's an timer or event, contains Timer ID that was received by setTimer
        int timerId
        
        // Deserialization from database file
        DatabaseEntry(JSONObject obj){
            type = Type.fromInt(obj.getInt("type"))
            time = obj.getLong("stamp")
            eventId = obj.getString("id")
            if(obj.has("soundPath"))
                soundPath = obj.getString("soundPath")
            else
                soundPath = null
        }
        
        DatabaseEntry(Type type, Calendar date, String name, String sound){
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
        
        // Serialization for database file
        JSONObject toJSON(){
            JSONObject obj = new JSONObject()
            obj.put("type", Type.toInt(type))
            obj.put("stamp", time)
            obj.put("id", eventId)
            if(soundPath!=null) obj.put("soundPath", soundPath)
            return obj
        }
        String getTimeString(){
            return new Date(time).format('dd.MM.yyyy HH:mm')
        }
    }
    
    static boolean isTimeInFuture(DatabaseEntry entry){
        return entry.time - Calendar.instance.getTimeInMillis() > 0
    }
    
    // All event entries contains here
    LinkedList entries
    
    // Creating special instance for watcher to prevent setting multiple watchers
    DatabaseEntry watcher
    
    // Plugin proxy instance, needs to be set inside "plugin.groovy"
    Object instance
    
    // Database filename
    private static filename
    
    Database(Object instance){
        entries = new LinkedList<DatabaseEntry>()
        this.instance = DatabaseEntry.instance = instance
        load()
    }
    
    List getListOfEntries(){
        def list=[]
        for (int i = 0; i < entries.size(); i++) {
            list.add(entries[i].eventId+" [ "+entries[i].getTimeString()+" ]")
        }
        return list
    }
    
    // Load all entries from database file
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
                    if (isTimeInFuture(entry)) {
                        add(entry)
                        notify(entry)
                    }
                }
            }
        } catch(Exception e){
            instance.log("Error while loading database for organizer")
        }
    }
    
    // Save all entries to filr
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
    
    // Delete selected entries and cancel all timers setted for them
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
    
    // Setting system timers for notifications about events
    def notify(Database.DatabaseEntry entry){
        def delay = entry.time - Calendar.instance.getTimeInMillis()

        entry.timerId = instance.setTimer(delay, { sender, data ->
            instance.sendMessage('DeskChan:notify',
                    [ 'message': instance.getString('notify.'+entry.type.toString().toLowerCase()).replace("#", entry.eventId),
                      'speech-intent': "ALARM",
                      'priority': 10000
                    ])
            //instance.sendMessage('DeskChan:request-say',[ 'intent': "ALARM", 'timeout': 20, 'priority': 10000, 'partible': false ])
            if(entry.soundPath!=null)
                instance.sendMessage('gui:play-sound',[ 'file': entry.soundPath, /*'count': 10*/ ])
            entries -= entry
            save()
            instance.setupEventsMenu()
        })
    }

}
