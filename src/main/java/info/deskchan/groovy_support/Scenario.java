package info.deskchan.groovy_support;

import groovy.lang.Closure;
import groovy.lang.Script;
import info.deskchan.core.Path;
import info.deskchan.core.PluginProperties;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core.ResponseListener;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class Scenario extends Script{

    /** Object containing last user input.
     *  It has complex map structure, so we store it here instead of giving it right to user. */
    private Answer answer;

    /** Default help message in answer dialog. */
    private static final String DEFAULT_HELP = ScenarioPlugin.pluginProxy.getString("write-anything");

    /** Variable containing is current thread was interrupted. **/
    private volatile boolean interrupted = false;

    /** Thread locking object. **/
    private final Locker lock = new Locker();
    
    private PluginProxyInterface pluginProxy = null;
    private Object data = null;

    /** Get data passed by owner of plugin. **/
    public Object getPassedData(){ return data; }

    /** Sets Plugin Proxy for this scenario. Should be called only from scenario initializer. **/
    protected void initialize(PluginProxyInterface proxy, Object data){
        if (pluginProxy != null)
            throw new RuntimeException("This method should be called only once and from scenario initializer, it's not accessable anymore.");
        this.pluginProxy = proxy;
        this.data = data;
    }


    // Standard plugin API

    protected void sendMessage(String tag) {
        pluginProxy.sendMessage(tag, null);
    }

    protected void sendMessage(String tag, Object data) {
        pluginProxy.sendMessage(tag, data);
    }

    protected String getString(String key){
        return pluginProxy.getString(key);
    }

    protected Path getDataDirPath() {
        return pluginProxy.getDataDirPath();
    }

    protected Path getPluginDirPath() {
        return pluginProxy.getPluginDirPath();
    }

    protected Path getRootDirPath() {
        return pluginProxy.getRootDirPath();
    }

    protected Path getAssetsDirPath() {
        return pluginProxy.getAssetsDirPath();
    }

    protected void log(Object text) {
        pluginProxy.log(text.toString());
    }

    protected void log(Throwable e) {
        pluginProxy.log(e);
    }

    protected PluginProperties getProperties() { return pluginProxy.getProperties(); }



    protected void alert(String text) {
        pluginProxy.sendMessage("DeskChan:show-technical", new HashMap(){{
            put("text", text);
        }});
    }
    protected void alert(String name, String text) {
        pluginProxy.sendMessage("DeskChan:show-technical", new HashMap(){{
            put("name", name);
            put("text", text);
            put("priority", messagePriority);
        }});
    }

    protected void say(String text){
        pluginProxy.sendMessage("DeskChan:request-say", new HashMap(){{
            put("text", text);
            put("characterImage", currentSprite);
            put("priority", messagePriority);
            put("skippable", false);
        }}, (sender, data) -> {
            pluginProxy.sendMessage("DeskChan:say", data, (sender1, data1) -> {
                lock.unlock();
            });
        });
        lock.lock();
    }

    protected void requestPhrase(String intent){
        pluginProxy.sendMessage("DeskChan:request-say", new HashMap(){{
            put("intent", intent);
            put("priority", messagePriority);
            put("skippable", false);
        }});
    }

    /** Current sprite forced by this scenario. **/
    private String currentSprite = "normal";
    protected void sprite(String text){
        currentSprite = text;
        pluginProxy.sendMessage("gui:set-image", text);
    }

    /** Priority of all messages sent by this scenario. All messages with lower priority will be skipped. **/
    private int messagePriority = 2500;
    protected int setMessagePriority(int val){
        return (messagePriority = val);
    }
    public int getMessagePriority(){ return messagePriority; }

    public ResponseListener interruptListener = new ResponseListener() {
        @Override
        public void handle(String sender, Object data) {
            pluginProxy.sendMessage("DeskChan:request-say", new HashMap(){{
                put("intent", "DONT_INTERRUPT");
                put("priority", messagePriority + 5);
            }});
        }
    };


    private int timerId = -1;
    protected synchronized void sleep(long delay){
        timerId = pluginProxy.setTimer(delay, (sender, d) -> {
            lock.unlock();
        });
        lock.lock();
    }


    // Getting input from user

    protected synchronized String receive(){
        return receive(null);
    }
    /** Request user speech with helpInfo as hint. **/
    protected synchronized String receive(Object helpInfo){
        pluginProxy.sendMessage("DeskChan:request-user-speech",
                helpInfo != null ? helpInfo : DEFAULT_HELP,
        (sender, data) -> {
            if (interrupted) {
                pluginProxy.sendMessage("DeskChan:discard-user-speech", data);
                lock.unlock();
                return;
            }
            if (data instanceof Map)
                answer = new Answer((Map) data);
            else
                answer = new Answer(data.toString());

            lock.unlock();
        });

        lock.lock();
        return answer.text;
    }

    protected synchronized Boolean receiveBoolean(){ return receiveBoolean(null); }
    protected synchronized Boolean receiveBoolean(Object helpInfo){
        receive(helpInfo);
        if (answer.intent == null) return null;
        for (String intent : answer.intent){
            if (intent.equals("YES") || intent.equals("ACCEPT") || intent.equals("DO_WORK"))
                return true;
            if (intent.equals("NO") || intent.equals("REFUSE"))
                return false;
        }
        return null;
    }

    private synchronized Object receiveDatatype(String datatype, Object helpInfo){
        receive(helpInfo);
        final List<Object> pr = new LinkedList<>();
        pluginProxy.sendMessage("speech:extract-data", new HashMap(){{
            put("speech", answer.text);
            put("type", datatype);
        }},
        (sender, data1) -> {
            pr.add(data1);
            lock.unlock();
        });

        lock.lock();
        return pr.size() > 0 ? pr.get(0) : null;
    }

    private synchronized Calendar receiveDatetype(String datatype, Object helpInfo){
        Long result = (Long) receiveDatatype(datatype, helpInfo);
        if (result == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(result);
        return cal;
    }

    protected synchronized Long receiveInteger(){ return receiveInteger(null); }
    protected synchronized Long receiveInteger(Object helpInfo){ return (Long) receiveDatatype("Integer", helpInfo); }

    protected synchronized Double receiveNumber(){ return receiveNumber(null); }
    protected synchronized Double receiveNumber(Object helpInfo){ return (Double) receiveDatatype("Number", helpInfo); }

    protected synchronized Calendar receiveDate(){ return receiveDate(null); }
    protected synchronized Calendar receiveDate(Object helpInfo){ return receiveDatetype("Date", helpInfo); }

    protected synchronized Calendar receiveDateTime(){ return receiveDate(null); }
    protected synchronized Calendar receiveDateTime(Object helpInfo){ return receiveDatetype("DateTime", helpInfo); }

    protected synchronized Calendar receiveTime(){ return receiveDate(null); }
    protected synchronized Calendar receiveTime(Object helpInfo){ return receiveDatetype("Time", helpInfo); }

    protected synchronized String getLastUserSpeech(){
        return answer.text;
    }

    // Checking some data (especially speech from user)

    private boolean whenCycle;
    void when(Object obj, Closure cl) {
        CaseCollector caseCollector = new CaseCollector();
        cl.setDelegate(caseCollector);
        cl.call();

        Function result = caseCollector.execute(obj);
        if (result != null)
            result.apply(obj);
    }
    void whenInput(Closure cl) {
        CaseCollector caseCollector = new CaseCollector();
        cl.setDelegate(caseCollector);
        cl.call();

        List<String> helpInfo = caseCollector.getHelpInfo();
        Object obj;
        while(true){
            receive(helpInfo);
            whenCycle = false;

            Function result = caseCollector.execute(answer);
            if (result != null)
                result.apply(answer);
            if(!whenCycle) break;
            cl.call();
        }
    }

    void again() {
        whenCycle = true;
    }
    private class CaseCollector{

        private Map<Object, Function> matches = new HashMap<>();
        private LinkedList<Object> queue = new LinkedList<>();

        class RegularRule { String rule; RegularRule(String rule){ this.rule = rule; } }

        private void clearQueue(Function action){
            for (Object obj : queue)
                matches.put(obj, action);
            queue.clear();
        }

        void equal(Object obj) {
            queue.add(obj);
        }

        void equal(Object[] obj) {
            for (Object o : obj)
                queue.add(o);
        }
        void equal(AbstractCollection<Object> obj) {
            queue.addAll(obj);
        }
        void equal(Object obj, Function action) {
            matches.put(obj, action);
            clearQueue(action);
        }
        void equal(Object[] obj, Function action) {
            for (Object o : obj)
                matches.put(o, action);
            clearQueue(action);
        }
        void equal(AbstractCollection<Object> obj, Function action) {
            for (Object o : obj)
                matches.put(o, action);
            clearQueue(action);
        }


        void match(String obj) {
            queue.add(new RegularRule(obj));
        }
        void match(String[] obj) {
            for (String o : obj)
                queue.add(new RegularRule(o));
        }
        void match(String[] obj, Function action) {
            for (String o : obj)
                matches.put(new RegularRule(o), action);
            clearQueue(action);
        }
        void match(String obj, Function action) {
            matches.put(new RegularRule(obj), action);
            clearQueue(action);
        }

        void otherwise(Function action) {
            matches.put(false, action);
            clearQueue(action);
        }

        Function execute(Object key) {

            // Taking "Otherwise"
            final AtomicReference<Function> action = new AtomicReference<>(matches.get(false));

            // If there is no equal/match blocks
            if(matches.size() == 0) return action.get();

            // Container for rules
            List<String> rules = new LinkedList<>();

            // intents comparison
            int maxVariantPriority = -1, vp;
            Function maxVariant = null;

            for(Map.Entry<Object, Function> entry : matches.entrySet()){
                if(entry.getKey() instanceof String){
                    vp = 0;
                    if (entry.getKey().equals(key) ||
                            (key instanceof Answer && (vp = ((Answer) key).intent.indexOf(entry.getKey())) >= 0)) {
                        if (maxVariantPriority < 0 || maxVariantPriority > vp) {
                            maxVariantPriority = vp;
                            maxVariant = entry.getValue();
                        }
                    }
                    continue;
                }
                if (entry.getKey() != null && entry.getKey().equals(key)){
                    return entry.getValue();
                }
                if(entry.getKey() instanceof RegularRule){
                    rules.add(((RegularRule) entry.getKey()).rule);
                }
            }

            if (maxVariant != null) return maxVariant;

            if ((key instanceof String || key instanceof Answer) && rules.size() > 0) {
                pluginProxy.sendMessage("speech:match-any", new HashMap<String, Object>() {{
                    put("speech", key instanceof Answer ? ((Answer) key).text : key.toString());
                    put("rules", rules);
                }}, (sender, data) -> {
                    Integer i = ((Number) data).intValue();

                    if (i >= 0) {
                        for (Map.Entry<Object, Function> entry : matches.entrySet()) {
                            if (entry.getKey() instanceof RegularRule && ((RegularRule) entry.getKey()).rule.equals(rules.get(i))) {
                                action.set(entry.getValue());
                                break;
                            }
                        }
                    }

                    lock.unlock();
                });
                lock.lock();
            }

            return action.get();
        }

        List<String> getHelpInfo(){
            if (matches.size() == 1 && matches.keySet().iterator().next().equals(false)){
                return null;
            }
            boolean isOtherwise = false;
            List<String> help = new ArrayList<>();
            for (Object an : matches.keySet()){
                if (an instanceof RegularRule)
                    help.add(((RegularRule) an).rule);
                else if (an.equals(false))
                    isOtherwise = true;
                else if (an instanceof String)
                    help.add("~" + pluginProxy.getString(an.toString()));
            }
            if (isOtherwise)
                help.add(pluginProxy.getString("other"));
            return help;
        }

    }

    protected void quit(){
        interrupted = true;
        if (timerId >= 0)
            pluginProxy.cancelTimer(timerId);
        throw new ScenarioPlugin.Companion.InterruptedScenarioException();
    }

    private static class Answer{

        String text;
        List<String> intent = null;

        Answer(String text){ this.text = text; }
        Answer(Map data){
            this.text = data.get("value") != null ? data.get("value").toString() : "";
            Object p = data.get("intent");
            if (p == null) return;
            if (p instanceof Collection){
                intent = new LinkedList<>((Collection) p);
            } else {
                intent = new LinkedList<>();
                intent.add(p.toString());
            }
        }

        public String toString(){ return text; }
    }

    private class Locker {

        boolean notified = false;

        public void lock(){
            if (!notified) {
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    quit();
                } catch (Throwable e) {
                    log(e);
                    quit();
                }
            }
            notified = false;
        }

        public void unlock(){
            notified = true;
            synchronized (this) {
                this.notify();
            }
        }
    }

    /** Working with character preset. **/

    public boolean instantCharacterInfluence = false;
    protected class Preset {
        private Map<String, Object> presetMap;
        Preset() {
            update();
        }
        Preset(Map<String, Object> map){
            presetMap = map;
        }

        public void update(){
            pluginProxy.sendMessage("talk:get-preset", true, (sender, data) ->  {
                presetMap = (Map) data;
                lock.unlock();
            });
            lock.lock();
        }

        public void save(){
            pluginProxy.sendMessage("talk:save-options", presetMap);
        }

        public Object getField(String key){  return presetMap.get("key");  }
        public void setField(String key, Object value){
            presetMap.put(key, value);
            if (instantCharacterInfluence) save();
        }

        public String getName(){  return (String) presetMap.get("name");  }
        public void setName(String newName){
            setField("name", newName);
            if (instantCharacterInfluence) save();
        }

        public Map getTags(){  return (Map) presetMap.get("tags");  }

        public Map getCharacter(){  return (Map) presetMap.get("character");  }
        public float getCharacterField(String key){  return ((Number) presetMap.get(key)).floatValue();      }
        public float setCharacterField(String key, float val){
            save();
            pluginProxy.sendMessage("talk:make-character-influence", new HashMap(){{
                put("feature", key);
                put("value", val - getCharacterField(key));
            }});
            update();
            return getCharacterField(key);
        }

        public float getManner(){  return getCharacterField("manner");  }
        public float getEnergy(){  return getCharacterField("energy");  }
        public float getEmpathy(){  return getCharacterField("empathy");  }
        public float getAttitude(){  return getCharacterField("attitude");  }
        public float getExperience(){  return getCharacterField("experience");  }
        public float getImpulsivity(){  return getCharacterField("impulsivity");  }
        public float getRelationship(){  return getCharacterField("relationship");  }

        public float setManner(float val){  return setCharacterField("manner", val);  }
        public float setEnergy(float val){  return setCharacterField("energy", val);  }
        public float setEmpathy(float val){  return setCharacterField("empathy", val);  }
        public float setAttitude(float val){  return setCharacterField("attitude", val);  }
        public float setExperience(float val){  return setCharacterField("experience", val);  }
        public float setImpulsivity(float val){  return setCharacterField("impulsivity", val);  }
        public float setRelationship(float val){  return setCharacterField("relationship", val);  }
        public float setSelfconfidence(float val){  return setCharacterField("selfconfidence", val);  }

        public float getSelfconfidence(){  return getCharacterField("selfconfidence");   }


        public void raiseEmotion(String key, String val){
            pluginProxy.sendMessage("talk:make-emotion-influence", new HashMap(){{
                put("emotion", key);
                put("value", val);
            }});
            update();
        }

        public String toString(){ return presetMap.toString(); }

    }
    private Preset currentPreset = null;

    public Preset getPreset(){
        if (currentPreset == null) currentPreset = new Preset();
        return currentPreset;
    }
    public Preset setPreset(Map newPreset){
        currentPreset = new Preset(newPreset);
        if (instantCharacterInfluence) currentPreset.save();
        return currentPreset;
    }
}